import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const IDFCConfig: BankConfig = {
  bankName: 'IDFC First Bank',
  bankCode: 'idfc',
  senders: {
    exact: ['IDFCBK'],
    contains: ['IDFCBK', 'IDFCFB'],
    patterns: [
      /^[A-Z]{2}-IDFCBK-[ST]$/,
      /^[A-Z]{2}-IDFCFB-[ST]$/,
      /^[A-Z]{2}-IDFCBK$/,
      /^[A-Z]{2}-IDFCFB$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'withdrawal'],
    income: ['credited', 'deposited', 'deposit']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class IDFCFirstBankParser extends BankParser {
  constructor() {
    super(IDFCConfig)
  }

  protected extractAmount(message: string): number | null {
    // Handle debit patterns - "debited by INR" format
    const debitPattern = /(?:is\s+)?debited\s+by\s+INR\s*([0-9,]+(?:\.\d{2})?)/i
    const debitMatch = message.match(debitPattern)
    if (debitMatch) {
      const amount = debitMatch[1].replace(/,/g, '')
      const parsed = parseFloat(amount)
      return isNaN(parsed) ? null : parsed
    }
    
    // Handle credit patterns - "credited by INR" format
    const creditPattern = /(?:is\s+)?credited\s+by\s+INR\s*([0-9,]+(?:\.\d{2})?)/i
    const creditMatch = message.match(creditPattern)
    if (creditMatch) {
      const amount = creditMatch[1].replace(/,/g, '')
      const parsed = parseFloat(amount)
      return isNaN(parsed) ? null : parsed
    }
    
    return super.extractAmount(message)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()
    
    if (lowerMessage.includes('debited')) return TransactionType.EXPENSE
    if (lowerMessage.includes('credited')) return TransactionType.INCOME
    if (lowerMessage.includes('withdrawn') || lowerMessage.includes('withdrawal')) {
      return TransactionType.EXPENSE
    }
    if (lowerMessage.includes('deposited') || lowerMessage.includes('deposit')) {
      return TransactionType.INCOME
    }
    
    return super.extractTransactionType(message)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // UPI transaction pattern
    if (message.match(/UPI/i)) {
      // Try to extract UPI ID
      const upiPattern = /(?:to|from|at)\s+([a-zA-Z0-9._-]+@[a-zA-Z0-9]+)/i
      const upiMatch = message.match(upiPattern)
      if (upiMatch) {
        return `UPI - ${upiMatch[1]}`
      }
      return 'UPI Transaction'
    }
    
    // NEFT/IMPS/RTGS patterns
    if (message.match(/NEFT/i)) return 'NEFT Transfer'
    if (message.match(/IMPS/i)) return 'IMPS Transfer'
    if (message.match(/RTGS/i)) return 'RTGS Transfer'
    
    // ATM withdrawal
    if (message.match(/ATM/i)) {
      const atmPattern = /ATM\s+(?:at\s+)?([^.]+?)(?:\.|,|on|New)/i
      const atmMatch = message.match(atmPattern)
      if (atmMatch) {
        const location = this.cleanMerchantName(atmMatch[1])
        return `ATM - ${location}`
      }
      return 'ATM Withdrawal'
    }
    
    // For card transactions
    const toPattern = /(?:to|at|for)\s+([A-Z][A-Z0-9\s&.-]+?)(?:\s+on|\s+New|\.|\,|$)/i
    const toMatch = message.match(toPattern)
    if (toMatch) {
      const merchant = this.cleanMerchantName(toMatch[1])
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }
    
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern: A/C XXXXXXXXXXX where last 4 digits are visible
    const acPattern = /A\/C\s+[X]*(\d{3,4})/i
    const acMatch = message.match(acPattern)
    if (acMatch) {
      const digits = acMatch[1]
      return digits.length >= 4 ? digits.slice(-4) : digits
    }
    
    return super.extractAccountLast4(message)
  }

  protected extractBalance(message: string): number | null {
    // Pattern: New Bal :INR XXXXX.00
    const balPattern = /New\s+Bal\s*:\s*INR\s*([0-9,]+(?:\.\d{2})?)/i
    const balMatch = message.match(balPattern)
    if (balMatch) {
      const balanceStr = balMatch[1].replace(/,/g, '')
      const parsed = parseFloat(balanceStr)
      return isNaN(parsed) ? null : parsed
    }
    
    return super.extractBalance(message)
  }

  protected extractReference(message: string): string | null {
    // UPI reference pattern
    const upiRefPattern = /UPI[:/]\s*([0-9]+)/i
    const upiRefMatch = message.match(upiRefPattern)
    if (upiRefMatch) {
      return upiRefMatch[1]
    }
    
    // Transaction ID pattern
    const txnIdPattern = /(?:txn|transaction)\s*(?:id|ref|no)[:\s]*([A-Z0-9]+)/i
    const txnIdMatch = message.match(txnIdPattern)
    if (txnIdMatch) {
      return txnIdMatch[1]
    }
    
    return super.extractReference(message)
  }

  protected isValidMerchantName(name: string): boolean {
    return name.length > 0 && name.length < 100 && !name.match(/^[0-9]+$/)
  }
}