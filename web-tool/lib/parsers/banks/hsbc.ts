import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const HSBCConfig: BankConfig = {
  bankName: 'HSBC Bank',
  bankCode: 'hsbc',
  senders: {
    exact: ['HSBC', 'HSBCIN'],
    contains: ['HSBC', 'HSBCIN'],
    patterns: [
      /^[A-Z]{2}-HSBCIN-[A-Z]$/,
      /^[A-Z]{2}-HSBC-[A-Z]$/
    ]
  },
  transactionType: {
    expense: ['is paid from', 'is debited', 'withdrawn'],
    income: ['is credited to', 'deposited']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class HSBCBankParser extends BankParser {
  constructor() {
    super(HSBCConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern: INR 49.00 is paid from
    // Pattern: INR 1000.50 is credited to
    const pattern = /INR\s+([\d,]+(?:\.\d{2})?)\s+is\s+(?:paid|credited|debited)/i
    const match = message.match(pattern)
    
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsed = parseFloat(amount)
      return isNaN(parsed) ? null : parsed
    }
    
    return super.extractAmount(message)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Pattern 1: "to [Merchant] on" for payments
    const paymentPattern = /to\s+([^.]+?)\s+on\s+\d/i
    const paymentMatch = message.match(paymentPattern)
    if (paymentMatch) {
      const merchant = this.cleanMerchantName(paymentMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }
    
    // Pattern 2: "from [Merchant]" for credits
    const creditPattern = /from\s+([^.]+?)(?:\s+on\s+|\s+with\s+|$)/i
    const creditMatch = message.match(creditPattern)
    if (creditMatch) {
      const merchant = this.cleanMerchantName(creditMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }
    
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern: account XXXXXX1234
    const pattern = /account\s+[X*]+(\d{4})/i
    const match = message.match(pattern)
    if (match) {
      return match[1]
    }
    
    return super.extractAccountLast4(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern: with ref 222222222222
    const pattern = /with\s+ref\s+(\w+)/i
    const match = message.match(pattern)
    if (match) {
      return match[1]
    }
    
    return super.extractReference(message)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()
    
    if (lowerMessage.includes('is paid from')) return TransactionType.EXPENSE
    if (lowerMessage.includes('is debited')) return TransactionType.EXPENSE
    if (lowerMessage.includes('is credited to')) return TransactionType.INCOME
    if (lowerMessage.includes('deposited')) return TransactionType.INCOME
    
    return super.extractTransactionType(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()
    
    // Check for HSBC-specific transaction keywords
    if (lowerMessage.includes('is paid from') ||
        lowerMessage.includes('is credited to') ||
        lowerMessage.includes('is debited') ||
        (lowerMessage.includes('inr') && lowerMessage.includes('account'))) {
      return true
    }
    
    return super.isTransactionMessage(message)
  }

  protected isValidMerchantName(name: string): boolean {
    return name.length > 0 && name.length < 100 && !name.match(/^[0-9]+$/)
  }
}