import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const PNBConfig: BankConfig = {
  bankName: 'Punjab National Bank',
  bankCode: 'pnb',
  senders: {
    exact: ['PNBBNK', 'PNB'],
    contains: ['PUNJAB NATIONAL BANK', 'PNBBNK', 'PUNBN'],
    patterns: [
      /^[A-Z]{2}-PNBBNK-S$/,
      /^[A-Z]{2}-PNB-S$/,
      /^[A-Z]{2}-PNBBNK$/,
      /^[A-Z]{2}-PNB$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'sent', 'paid'],
    income: ['credited', 'deposited', 'received', 'has been credited']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class PNBParser extends BankParser {
  constructor() {
    super(PNBConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: debited INR 500.00
    const debitPattern = /debited\s+INR\s+([0-9,]+(?:\.\d{2})?)/i
    let match = message.match(debitPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 2: INR 1000.00 has been credited
    const creditPattern = /INR\s+([0-9,]+(?:\.\d{2})?)\s+has\s+been\s+credited/i
    match = message.match(creditPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 3: Bal Rs.5000.00 CR
    const balCreditPattern = /Bal\s+Rs\.?([0-9,]+(?:\.\d{2})?)\s+CR/i
    match = message.match(balCreditPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    return super.extractAmount(message)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Pattern: From MERCHANT NAME/
    const fromPattern = /From\s+([^/]+)\//i
    const match = message.match(fromPattern)
    if (match) {
      const merchant = this.cleanMerchantName(match[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    if (message.toLowerCase().includes('neft')) {
      return 'NEFT Transfer'
    }

    if (message.toLowerCase().includes('upi')) {
      return 'UPI Transaction'
    }

    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern: A/c XX1234 or A/c X****1234
    const acPattern = /A\/c\s+(?:XX|X\*+)?(\d{4})/i
    const match = message.match(acPattern)
    if (match) {
      return match[1]
    }

    return super.extractAccountLast4(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern 1: ref no. ABC123456
    const neftRefPattern = /ref\s+no\.\s+([A-Z0-9]+)/i
    let match = message.match(neftRefPattern)
    if (match) {
      return match[1]
    }

    // Pattern 2: UPI: 123456789012
    const upiRefPattern = /UPI:\s*([0-9]+)/i
    match = message.match(upiRefPattern)
    if (match) {
      return match[1]
    }

    return super.extractReference(message)
  }

  protected extractBalance(message: string): number | null {
    // Pattern: Bal INR 50000.00 or Bal Rs.25000.00
    const balPattern = /Bal\s+(?:INR\s+|Rs\.?)([0-9,]+(?:\.\d{2})?)/i
    const match = message.match(balPattern)
    if (match) {
      const balanceStr = match[1].replace(/,/g, '')
      const balance = parseFloat(balanceStr)
      if (!isNaN(balance)) {
        return balance
      }
    }

    return super.extractBalance(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Special case for e-statement registration messages
    if (lowerMessage.includes('register for e-statement')) {
      return true
    }

    return super.isTransactionMessage(message)
  }
}