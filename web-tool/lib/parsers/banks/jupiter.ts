import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const JupiterConfig: BankConfig = {
  bankName: 'Jupiter',
  bankCode: 'jupiter',
  senders: {
    exact: [],
    contains: [],
    patterns: [
      /^[A-Z]{2}-JTEDGE-S$/,
      /^[A-Z]{2}-JTEDGE-T$/,
      /^[A-Z]{2}-JTEDGE$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'sent', 'paid'],
    income: ['credited', 'deposited', 'received']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'to dispute',
    'offer',
    'discount'
  ]
}

export class JupiterParser extends BankParser {
  constructor() {
    super(JupiterConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: "Rs.130.00 debited"
    const debitPattern = /Rs\.?\s*([0-9,]+(?:\.\d{2})?)\s+debited/i
    let match = message.match(debitPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 2: "Rs.XXX credited"
    const creditPattern = /Rs\.?\s*([0-9,]+(?:\.\d{2})?)\s+credited/i
    match = message.match(creditPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Fall back to base class patterns
    return super.extractAmount(message)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // For Jupiter/CSB credit card transactions, the merchant info is usually not in the message
    // These are typically just marked as credit card transactions
    
    const lowerMessage = message.toLowerCase()

    // Check for specific transaction types
    if (lowerMessage.includes('edge csb bank rupay credit card')) return 'Credit Card Payment'
    if (lowerMessage.includes('jupiter csb edge')) return 'Credit Card Payment'
    if (lowerMessage.includes('credit card')) return 'Credit Card Payment'
    if (lowerMessage.includes('upi')) return 'UPI Transaction'

    return super.extractMerchant(message, sender) || 'Jupiter Transaction'
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern 1: "ending 6852"
    const endingPattern = /ending\s+(\d{4})/i
    let match = message.match(endingPattern)
    if (match) {
      return match[1]
    }

    // Pattern 2: "Card ending 6852"
    const cardEndingPattern = /Card\s+ending\s+(\d{4})/i
    match = message.match(cardEndingPattern)
    if (match) {
      return match[1]
    }

    // Fall back to base class
    return super.extractAccountLast4(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern: "UPI Ref no.281751568470"
    const upiRefPattern = /UPI\s+Ref\s+no\.?\s*([A-Za-z0-9]+)/i
    const match = message.match(upiRefPattern)
    if (match) {
      return match[1]
    }

    // Fall back to base class
    return super.extractReference(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Skip dispute instructions (not a transaction)
    if (lowerMessage.includes('to dispute') && lowerMessage.includes('call')) {
      // This is just instruction text, but don't skip the entire message
      // Let base class handle the rest
    }

    // Check for Jupiter-specific transaction keywords
    if (lowerMessage.includes('jupiter') || lowerMessage.includes('csb')) {
      // If it's from Jupiter/CSB and has transaction keywords, it's likely valid
      return super.isTransactionMessage(message)
    }

    // Fall back to base class for standard checks
    return super.isTransactionMessage(message)
  }
}