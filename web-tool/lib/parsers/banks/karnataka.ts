import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const KarnatakaBankConfig: BankConfig = {
  bankName: 'Karnataka Bank',
  bankCode: 'karnataka',
  senders: {
    exact: ['KBLBNK', 'KARBANK'],
    contains: ['KARNATAKA BANK', 'KARNATAKABANK', 'KBLBNK', 'KTKBANK', 'KARBANK'],
    patterns: [
      /^[A-Z]{2}-KBLBNK-S$/,
      /^[A-Z]{2}-KARBANK-S$/,
      /^[A-Z]{2}-KBLBNK$/
    ]
  },
  transactionType: {
    expense: ['debited for', 'debited', 'withdrawn'],
    income: ['credited by', 'credited', 'deposited']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class KarnatakaBankParser extends BankParser {
  constructor() {
    super(KarnatakaBankConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: "DEBITED for Rs.6368/-"
    const debitPattern = /DEBITED\s+for\s+Rs\.?([0-9,]+(?:\.\d{2})?)\/?-?/i
    let match = message.match(debitPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 2: "credited by Rs.6600.00"
    const creditPattern = /credited\s+by\s+Rs\.?([0-9,]+(?:\.\d{2})?)/i
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
    // Pattern 1: ACH transactions - "ACHInwDr-MERCHANT/date"
    const achPattern = /ACH[A-Za-z]*-([^/]+)\//i
    let match = message.match(achPattern)
    if (match) {
      const merchant = this.cleanMerchantName(match[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 2: "from <merchant> on" for UPI
    const fromPattern = /from\s+([^\s]+)\s+on/i
    match = message.match(fromPattern)
    if (match) {
      const merchant = this.cleanMerchantName(match[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 3: Check for specific transaction types
    const lowerMessage = message.toLowerCase()
    if (lowerMessage.includes('lic of india')) return 'LIC of India'
    if (lowerMessage.includes('upi') && !fromPattern.test(message)) return 'UPI Transaction'

    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern 1: "Account x001234x" or "Account XX1234X"
    const accountPattern1 = /Account\s+[xX]*([0-9]{4,6})[xX]*/i
    let match = message.match(accountPattern1)
    if (match) {
      const digits = match[1]
      // Return last 4 digits if more than 4
      return digits.length > 4 ? digits.slice(-4) : digits
    }

    // Pattern 2: "a/c XX1234"
    const accountPattern2 = /a\/c\s+[xX]{0,2}([0-9]{4,6})/i
    match = message.match(accountPattern2)
    if (match) {
      return match[1].slice(-4)
    }

    // Fall back to base class
    return super.extractAccountLast4(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern 1: "UPI Ref no 441877242175"
    const upiRefPattern = /UPI\s+Ref\s+no\s+([0-9]+)/i
    const match = message.match(upiRefPattern)
    if (match) {
      return match[1]
    }

    // Fall back to base class
    return super.extractReference(message)
  }

  protected extractBalance(message: string): number | null {
    // Pattern: "Balance is Rs.705.92"
    const balancePattern = /Balance\s+is\s+Rs\.?([0-9,]+(?:\.\d{2})?)/i
    const match = message.match(balancePattern)
    if (match) {
      const balanceStr = match[1].replace(/,/g, '')
      const balance = parseFloat(balanceStr)
      if (!isNaN(balance)) {
        return balance
      }
    }

    // Fall back to base class
    return super.extractBalance(message)
  }
}