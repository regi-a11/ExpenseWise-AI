import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const IDBIBankConfig: BankConfig = {
  bankName: 'IDBI Bank',
  bankCode: 'idbi',
  senders: {
    exact: ['IDBIBK', 'IDBIBANK'],
    contains: ['IDBIBK', 'IDBIBANK', 'IDBI'],
    patterns: [
      /^[A-Z]{2}-IDBIBK-S$/,
      /^[A-Z]{2}-IDBI-S$/,
      /^[A-Z]{2}-IDBIBK$/,
      /^[A-Z]{2}-IDBI$/
    ]
  },
  transactionType: {
    expense: ['debited with', 'debited for', 'debited', 'sent'],
    income: ['credited with', 'credited', 'deposited', 'received']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'to block upi',
    'offer',
    'discount'
  ]
}

export class IDBIBankParser extends BankParser {
  constructor() {
    super(IDBIBankConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: "debited with Rs 59.00"
    const debitWithPattern = /debited\s+with\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)/i
    let match = message.match(debitWithPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 2: "debited for Rs 1040.00"
    const debitForPattern = /debited\s+for\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)/i
    match = message.match(debitForPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 3: "credited with Rs XXX"
    const creditPattern = /credited\s+with\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)/i
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
    // Pattern 1: "towards <merchant> for"
    const towardsPattern = /towards\s+([^.\n]+?)\s+for/i
    let match = message.match(towardsPattern)
    if (match) {
      const merchant = this.cleanMerchantName(match[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 2: "; <merchant> credited."
    const creditedMerchantPattern = /;\s*([^.\n]+?)\s+credited\./i
    match = message.match(creditedMerchantPattern)
    if (match) {
      const merchant = this.cleanMerchantName(match[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 3: AutoPay/Mandate specific
    if (message.toLowerCase().includes('autopay') || 
        message.toLowerCase().includes('mandate')) {
      // Extract merchant name before "for" if it's AutoPay
      const merchantPattern = /towards\s+([^.\n]+?)\s+for\s+\w*MANDATE/i
      match = message.match(merchantPattern)
      if (match) {
        return this.cleanMerchantName(match[1].trim())
      }
    }

    // Fall back to base class patterns
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern 1: "Acct XX1234"
    const acctPattern = /Acct\s+(?:XX|X\*+)?(\d{3,4})/i
    let match = message.match(acctPattern)
    if (match) {
      return match[1]
    }

    // Pattern 2: "IDBI Bank Acct XX1234"
    const bankAcctPattern = /IDBI\s+Bank\s+Acct\s+(?:XX|X\*+)?(\d{3,4})/i
    match = message.match(bankAcctPattern)
    if (match) {
      return match[1]
    }

    // Fall back to base class
    return super.extractAccountLast4(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern 1: "RRN 519766155631"
    const rrnPattern = /RRN\s+([A-Za-z0-9]+)/i
    let match = message.match(rrnPattern)
    if (match) {
      return match[1]
    }

    // Pattern 2: "UPI:521687538121"
    const upiPattern = /UPI:([A-Za-z0-9]+)/i
    match = message.match(upiPattern)
    if (match) {
      return match[1]
    }

    // Fall back to base class
    return super.extractReference(message)
  }

  protected extractBalance(message: string): number | null {
    // Pattern: "Bal Rs 3694.38"
    const balancePattern = /Bal\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)/i
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

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Skip UPI block instructions (not a transaction)
    if (lowerMessage.includes('to block upi') && lowerMessage.includes('send sms')) {
      // This is just instruction text, but don't skip the entire message
      // Let base class handle the rest
    }

    // Fall back to base class for standard checks
    return super.isTransactionMessage(message)
  }
}