import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const BankOfBarodaConfig: BankConfig = {
  bankName: 'Bank of Baroda',
  bankCode: 'bob',
  senders: {
    exact: ['BOB', 'BANKOFBARODA'],
    contains: ['BOB', 'BARODA', 'BOBSMS', 'BOBTXN'],
    patterns: [
      /^[A-Z]{2}-BOBSMS-[A-Z]$/,
      /^[A-Z]{2}-BOBTXN-[A-Z]$/,
      /^[A-Z]{2}-BOB-[A-Z]$/
    ]
  },
  transactionType: {
    expense: ['dr.', 'debited', 'dr. from'],
    income: ['cr.', 'credited', 'deposited', 'credited with', 'credited to']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class BankOfBarodaParser extends BankParser {
  constructor() {
    super(BankOfBarodaConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: Rs.80.00 Dr. from
    const drPattern = /Rs\.?\s*([\d,]+(?:\.\d{2})?)\s+Dr\.?\s+from/i
    let match = message.match(drPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 2: credited with INR 70.00
    const creditPattern = /credited\s+with\s+INR\s+([\d,]+(?:\.\d{2})?)/i
    match = message.match(creditPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 3: Rs.xxxxxx Credited to
    const creditPattern2 = /Rs\.?\s*([\d,]+(?:\.\d{2})?)\s+Credited\s+to/i
    match = message.match(creditPattern2)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 4: Cr. to redacted@ybl (UPI)
    const crPattern = /Rs\.?\s*([\d,]+(?:\.\d{2})?)\s+.*?Cr\.?\s+to/i
    match = message.match(crPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 5: Rs.xxxxx deposited in cash
    const cashDepositPattern = /Rs\.?\s*([\d,]+(?:\.\d{2})?)\s+deposited\s+in\s+cash/i
    match = message.match(cashDepositPattern)
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
    // Pattern 1: Cr. to redacted@ybl (UPI VPA)
    const upiPattern = /Cr\.?\s+to\s+([^\s]+@[^\s.]+)/i
    const upiMatch = message.match(upiPattern)
    if (upiMatch) {
      const vpa = upiMatch[1]
      // Extract name from VPA if possible
      const name = vpa.split('@')[0]
      if (name === 'redacted') {
        return 'UPI Payment'
      } else {
        return this.cleanMerchantName(name)
      }
    }

    // Pattern 2: IMPS by Name of Person
    const impsPattern = /IMPS\/[\d]+\s+by\s+([^.]+?)(?:\s*\.|$)/i
    const impsMatch = message.match(impsPattern)
    if (impsMatch) {
      const merchant = this.cleanMerchantName(impsMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 3: For UPI credits, extract from context
    if (message.toLowerCase().includes('upi')) {
      if (message.toLowerCase().includes('credited')) {
        return 'UPI Credit'
      } else if (message.toLowerCase().includes('dr.')) {
        return 'UPI Payment'
      }
    }

    // Pattern 4: For IMPS without clear merchant
    if (message.toLowerCase().includes('imps')) {
      return 'IMPS Transfer'
    }

    // Pattern 5: Cash deposit
    if (message.toLowerCase().includes('deposited in cash')) {
      return 'Cash Deposit'
    }

    // Fall back to base class patterns
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern 1: A/C XXXXXX (6 digits shown)
    const sixDigitPattern = /A\/C\s+X*(\d{6})/i
    let match = message.match(sixDigitPattern)
    if (match) {
      const digits = match[1]
      // Return last 4 of the 6 digits shown
      return digits.slice(-4)
    }

    // Pattern 2: A/c ...xxxx
    const maskedPattern = /A\/c\s+\.+(\d{4})/i
    match = message.match(maskedPattern)
    if (match) {
      return match[1]
    }

    return super.extractAccountLast4(message)
  }

  protected extractBalance(message: string): number | null {
    // Pattern 1: AvlBal:Rsxxxxxcx or AvlBal: Rsxxxxxxx
    const avlBalPattern = /AvlBal:\s*Rs\.?\s*([\d,]+(?:\.\d{2})?)/i
    let match = message.match(avlBalPattern)
    if (match) {
      const balanceStr = match[1].replace(/,/g, '')
      const balance = parseFloat(balanceStr)
      if (!isNaN(balance)) {
        return balance
      }
    }

    // Pattern 2: Total Bal:Rs.xxxxxxx
    const totalBalPattern = /Total\s+Bal:\s*Rs\.?\s*([\d,]+(?:\.\d{2})?)/i
    match = message.match(totalBalPattern)
    if (match) {
      const balanceStr = match[1].replace(/,/g, '')
      const balance = parseFloat(balanceStr)
      if (!isNaN(balance)) {
        return balance
      }
    }

    // Pattern 3: Avlbl Amt:Rs.xxxxxxxx
    const avlAmtPattern = /Avlbl\s+Amt:\s*Rs\.?\s*([\d,]+(?:\.\d{2})?)/i
    match = message.match(avlAmtPattern)
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
    // Pattern 1: Ref:52211xxxxxx
    const refPattern1 = /Ref:\s*(\d+)/i
    let match = message.match(refPattern1)
    if (match) {
      return match[1]
    }

    // Pattern 2: UPI Ref No 510xxxxxxxxxx
    const upiRefPattern = /UPI\s+Ref\s+No\s+(\d+)/i
    match = message.match(upiRefPattern)
    if (match) {
      return match[1]
    }

    // Pattern 3: IMPS/5182xxxxxxx
    const impsRefPattern = /IMPS\/(\d+)/i
    match = message.match(impsRefPattern)
    if (match) {
      return match[1]
    }

    return super.extractReference(message)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()

    if (lowerMessage.includes('dr.') || lowerMessage.includes('debited')) {
      return TransactionType.EXPENSE
    }
    if (lowerMessage.includes('cr.') || lowerMessage.includes('credited')) {
      return TransactionType.INCOME
    }
    if (lowerMessage.includes('deposited')) {
      return TransactionType.INCOME
    }

    return super.extractTransactionType(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Check for BOB-specific transaction keywords
    if (lowerMessage.includes('dr. from') || 
        lowerMessage.includes('cr. to') ||
        lowerMessage.includes('credited to a/c') ||
        lowerMessage.includes('credited with inr') ||
        lowerMessage.includes('deposited in cash')) {
      return true
    }

    return super.isTransactionMessage(message)
  }
}