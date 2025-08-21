import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const CanaraConfig: BankConfig = {
  bankName: 'Canara Bank',
  bankCode: 'canara',
  senders: {
    exact: [],
    contains: ['CANBNK', 'CANARA'],
    patterns: []
  },
  transactionType: {
    expense: ['paid', 'debited', 'has been debited'],
    income: ['credited', 'has been credited', 'deposited']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'failed due to',
    'offer',
    'discount'
  ]
}

export class CanaraParser extends BankParser {
  constructor() {
    super(CanaraConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern: Rs.23.00 paid thru
    const upiAmountPattern = /Rs\.?\s*([\d,]+(?:\.\d{2})?)\s+paid/i
    let match = message.match(upiAmountPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern: INR 50.00 has been DEBITED
    const debitPattern = /INR\s+([\d,]+(?:\.\d{2})?)\s+has\s+been\s+DEBITED/i
    match = message.match(debitPattern)
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
    // Pattern: paid thru A/C XX1234 on 08-8-25 16:41:00 to BMTC BUS KA57F6
    const upiMerchantPattern = /\sto\s+([^,]+?)(?:,\s*UPI|\.|-Canara)/i
    const match = message.match(upiMerchantPattern)
    if (match) {
      const merchant = this.cleanMerchantName(match[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Check if it's a generic debit
    if (message.toLowerCase().includes('debited')) {
      return 'Canara Bank Debit'
    }

    // Fall back to base class patterns
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern: account XXX123 or A/C XX1234
    const accountPattern = /(?:account|A\/C)\s+(?:XX|X\*+)?(\d{3,4})/i
    const match = message.match(accountPattern)
    if (match) {
      return match[1]
    }

    return super.extractAccountLast4(message)
  }

  protected extractBalance(message: string): number | null {
    // Pattern: Total Avail.bal INR 1,092.62
    const balancePattern = /(?:Total\s+)?Avail\.?bal\s+INR\s+([\d,]+(?:\.\d{2})?)/i
    const match = message.match(balancePattern)
    if (match) {
      const balanceStr = match[1].replace(/,/g, '')
      const balance = parseFloat(balanceStr)
      if (!isNaN(balance)) {
        return balance
      }
    }

    return super.extractBalance(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern: UPI Ref 123456789012
    const upiRefPattern = /UPI\s+Ref\s+(\d+)/i
    const match = message.match(upiRefPattern)
    if (match) {
      return match[1]
    }

    return super.extractReference(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Skip failed transactions
    if (lowerMessage.includes('failed due to')) {
      return false
    }

    // Check for Canara-specific transaction keywords
    if (lowerMessage.includes('paid thru') || 
        lowerMessage.includes('has been debited') ||
        lowerMessage.includes('has been credited')) {
      return true
    }

    return super.isTransactionMessage(message)
  }
}