import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const JioPaymentsBankConfig: BankConfig = {
  bankName: 'Jio Payments Bank',
  bankCode: 'jiopayments',
  senders: {
    exact: [],
    contains: ['JIOPBS'],
    patterns: []
  },
  transactionType: {
    expense: ['debited with', 'sent from', 'upi/dr'],
    income: ['credited with', 'upi/cr']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class JioPaymentsBankParser extends BankParser {
  constructor() {
    super(JioPaymentsBankConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: credited with Rs.1670.00
    const creditPattern = /credited\s+with\s+Rs\.?\s*([\d,]+(?:\.\d{2})?)/i
    let match = message.match(creditPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 2: Rs. 1170.00 Sent from
    const sentPattern = /Rs\.?\s*([\d,]+(?:\.\d{2})?)\s+Sent\s+from/i
    match = message.match(sentPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 3: debited with Rs. 1750.00
    const debitPattern = /debited\s+with\s+Rs\.?\s*([\d,]+(?:\.\d{2})?)/i
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
    // Pattern 1: UPI/CR/700003371002/AMAN KU
    // Pattern 2: UPI/DR/520300007125/AMAN KUM
    const upiPattern = /UPI\/(?:CR|DR)\/[\d]+\/([^.\n]+?)(?:\s*\.|$)/i
    const match = message.match(upiPattern)
    if (match) {
      const merchant = this.cleanMerchantName(match[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // If no specific merchant found, check transaction type
    if (message.toLowerCase().includes('upi/cr')) return 'UPI Credit'
    if (message.toLowerCase().includes('upi/dr')) return 'UPI Payment'
    if (message.toLowerCase().includes('sent from')) return 'Money Transfer'

    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern 1: JPB A/c x4288
    const jpbPattern = /JPB\s+A\/c\s+x(\d{4})/i
    let match = message.match(jpbPattern)
    if (match) {
      return match[1]
    }

    // Pattern 2: from x4288
    const fromPattern = /from\s+x(\d{4})/i
    match = message.match(fromPattern)
    if (match) {
      return match[1]
    }

    return super.extractAccountLast4(message)
  }

  protected extractBalance(message: string): number | null {
    // Pattern: Avl. Bal: Rs. 9095.5
    const balancePattern = /Avl\.?\s*Bal:\s*Rs\.?\s*([\d,]+(?:\.\d{1,2})?)/i
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
    // Pattern: UPI/CR/700003371002 or UPI/DR/520300007125
    const upiRefPattern = /UPI\/(?:CR|DR)\/(\d+)/i
    const match = message.match(upiRefPattern)
    if (match) {
      return match[1]
    }

    return super.extractReference(message)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()

    if (lowerMessage.includes('credited')) return TransactionType.INCOME
    if (lowerMessage.includes('upi/cr')) return TransactionType.INCOME
    if (lowerMessage.includes('debited')) return TransactionType.EXPENSE
    if (lowerMessage.includes('upi/dr')) return TransactionType.EXPENSE
    if (lowerMessage.includes('sent from')) return TransactionType.EXPENSE

    return super.extractTransactionType(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Check for Jio Payments Bank specific transaction keywords
    if (lowerMessage.includes('jpb a/c') || 
        lowerMessage.includes('upi/cr') ||
        lowerMessage.includes('upi/dr') ||
        lowerMessage.includes('sent from')) {
      return true
    }

    return super.isTransactionMessage(message)
  }
}