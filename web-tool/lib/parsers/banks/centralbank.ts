import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const CentralBankConfig: BankConfig = {
  bankName: 'Central Bank of India',
  bankCode: 'centralbank',
  senders: {
    exact: ['CENTBK', 'CBOI', 'CENTRALBANK', 'CENTRAL'],
    contains: ['CENTBK', 'CBOI', 'CENTRALBANK', 'CENTRAL'],
    patterns: [
      /^[A-Z]{2}-CENTBK-[A-Z]$/,
      /^[A-Z]{2}-CBOI-[A-Z]$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'paid'],
    income: ['credited', 'deposited', 'received']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class CentralBankOfIndiaParser extends BankParser {
  constructor() {
    super(CentralBankConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: Credited by Rs.50.00 or Debited by Rs.100.50
    const pattern1 = /(?:Credited|Debited)\s+by\s+Rs\.?\s*([\d,]+(?:\.\d{2})?)/i
    const match1 = message.match(pattern1)
    if (match1) {
      const amount = match1[1].replace(/,/g, '')
      const parsed = parseFloat(amount)
      return isNaN(parsed) ? null : parsed
    }
    
    // Pattern 2: Rs.XXX credited/debited
    const pattern2 = /Rs\.?\s*([\d,]+(?:\.\d{2})?)\s+(?:credited|debited)/i
    const match2 = message.match(pattern2)
    if (match2) {
      const amount = match2[1].replace(/,/g, '')
      const parsed = parseFloat(amount)
      return isNaN(parsed) ? null : parsed
    }
    
    return super.extractAmount(message)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Pattern 1: "from [NAME]" for credits
    const fromPattern = /from\s+([A-Z0-9]+|[^\s]+?)(?:\s+via|\s+Ref|\s+\.|$)/i
    const fromMatch = message.match(fromPattern)
    if (fromMatch) {
      const merchant = fromMatch[1].trim()
      // Handle masked UPI IDs
      if (merchant.includes('X')) {
        return 'UPI Transfer'
      }
      return this.cleanMerchantName(merchant)
    }
    
    // Pattern 2: "to [NAME]" for debits
    const toPattern = /to\s+([^\s]+?)(?:\s+via|\s+Ref|\s+\.|$)/i
    const toMatch = message.match(toPattern)
    if (toMatch) {
      const merchant = this.cleanMerchantName(toMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }
    
    // Pattern 3: via UPI
    if (message.match(/via UPI/i)) {
      if (message.match(/Credited/i)) {
        return 'UPI Credit'
      } else if (message.match(/Debited/i)) {
        return 'UPI Payment'
      }
    }
    
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern 1: account XX3113 (last 4 visible)
    const pattern1 = /account\s+[X*]*(\d{4})/i
    const match1 = message.match(pattern1)
    if (match1) {
      return match1[1]
    }
    
    // Pattern 2: A/C ending XXXX
    const pattern2 = /A\/C\s+ending\s+[X*]*(\d{4})/i
    const match2 = message.match(pattern2)
    if (match2) {
      return match2[1]
    }
    
    return super.extractAccountLast4(message)
  }

  protected extractBalance(message: string): number | null {
    // Pattern 1: Total Bal Rs.0000.99 CR
    const totalBalPattern = /Total\s+Bal\s+Rs\.?\s*([\d,]+(?:\.\d{2})?)\s+(CR|DR)/i
    const totalBalMatch = message.match(totalBalPattern)
    if (totalBalMatch) {
      const balanceStr = totalBalMatch[1].replace(/,/g, '')
      const type = totalBalMatch[2].toUpperCase()
      const parsed = parseFloat(balanceStr)
      if (!isNaN(parsed)) {
        // If DR (debit), make it negative
        return type === 'DR' ? -parsed : parsed
      }
    }
    
    // Pattern 2: Clear Bal Rs.XXX CR
    const clearBalPattern = /Clear\s+Bal\s+Rs\.?\s*([\d,]+(?:\.\d{2})?)\s+(CR|DR)/i
    const clearBalMatch = message.match(clearBalPattern)
    if (clearBalMatch) {
      const balanceStr = clearBalMatch[1].replace(/,/g, '')
      const type = clearBalMatch[2].toUpperCase()
      const parsed = parseFloat(balanceStr)
      if (!isNaN(parsed)) {
        return type === 'DR' ? -parsed : parsed
      }
    }
    
    return super.extractBalance(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern: Ref No.541986000003
    const pattern = /Ref\s+No\.?\s*(\w+)/i
    const match = message.match(pattern)
    if (match) {
      return match[1]
    }
    
    return super.extractReference(message)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()
    
    if (lowerMessage.includes('credited')) return TransactionType.INCOME
    if (lowerMessage.includes('deposited')) return TransactionType.INCOME
    if (lowerMessage.includes('received')) return TransactionType.INCOME
    if (lowerMessage.includes('debited')) return TransactionType.EXPENSE
    if (lowerMessage.includes('withdrawn')) return TransactionType.EXPENSE
    if (lowerMessage.includes('paid')) return TransactionType.EXPENSE
    
    return super.extractTransactionType(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()
    
    // Check for CBoI-specific transaction keywords
    if ((lowerMessage.includes('credited by') || 
         lowerMessage.includes('debited by')) &&
        lowerMessage.includes('bal')) {
      return true
    }
    
    // Check for signature
    if (lowerMessage.includes('-cboi')) {
      return lowerMessage.includes('credited') || 
             lowerMessage.includes('debited')
    }
    
    return super.isTransactionMessage(message)
  }

  protected isValidMerchantName(name: string): boolean {
    return name.length > 0 && name.length < 100 && !name.match(/^[0-9]+$/)
  }
}