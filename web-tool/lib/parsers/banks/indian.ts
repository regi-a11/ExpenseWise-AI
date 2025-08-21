import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const IndianBankConfig: BankConfig = {
  bankName: 'Indian Bank',
  bankCode: 'indian',
  senders: {
    exact: ['INDBMK', 'INDIANBK', 'INDIAN'],
    contains: ['INDIAN BANK', 'INDIANBANK', 'INDIANBK', 'INDBMK'],
    patterns: [
      /^[A-Z]{2}-INDBMK-S$/,
      /^[A-Z]{2}-INDIANBK-S$/,
      /^[A-Z]{2}-INDBMK-[TPG]$/,
      /^[A-Z]{2}-INDIANBK-[TPG]$/,
      /^[A-Z]{2}-INDBMK$/,
      /^[A-Z]{2}-INDIANBK$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'upi payment', 'paid', 'purchase'],
    income: ['credited', 'deposited', 'received']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'mandate',
    'upcoming',
    'will be debited'
  ]
}

interface MandateInfo {
  amount: number
  nextDeductionDate: string | null
  merchant: string
}

export class IndianBankParser extends BankParser {
  constructor() {
    super(IndianBankConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: debited Rs. 19000.00
    const debitPattern = /debited\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    let match = message.match(debitPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 2: credited Rs. 5000.00
    const creditPattern = /credited\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    match = message.match(creditPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 3: withdrawn Rs. 2000
    const withdrawnPattern = /withdrawn\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    match = message.match(withdrawnPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 4: UPI payment of Rs. 500
    const upiPattern = /UPI\s+payment\s+of\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    match = message.match(upiPattern)
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
    // Pattern 1: "to Sumit Choudhary"
    const toPattern = /to\s+([^.\n]+?)(?:\.\s*UPI:|UPI:|$)/i
    const toMatch = message.match(toPattern)
    if (toMatch) {
      const merchant = this.cleanMerchantName(toMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 2: "from John Doe"
    const fromPattern = /from\s+([^.\n]+?)(?:\.\s*UPI:|UPI:|$)/i
    const fromMatch = message.match(fromPattern)
    if (fromMatch) {
      const merchant = this.cleanMerchantName(fromMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 3: ATM withdrawal at location
    const atmPattern = /ATM\s+(?:withdrawal\s+)?at\s+([^.\n]+?)(?:\s+on|$)/i
    const atmMatch = message.match(atmPattern)
    if (atmMatch) {
      const location = this.cleanMerchantName(atmMatch[1].trim())
      if (this.isValidMerchantName(location)) {
        return `ATM - ${location}`
      }
    }

    // Fall back to base class patterns
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern 1: A/c *1234
    const pattern1 = /A\/c\s+\*(\d{4})/i
    let match = message.match(pattern1)
    if (match) {
      return match[1]
    }

    // Pattern 2: Account XX1234 or XXXX1234
    const pattern2 = /Account\s+X*(\d{4})/i
    match = message.match(pattern2)
    if (match) {
      return match[1]
    }

    // Pattern 3: A/c ending 1234
    const pattern3 = /A\/c\s+ending\s+(\d{4})/i
    match = message.match(pattern3)
    if (match) {
      return match[1]
    }

    // Fall back to base class
    return super.extractAccountLast4(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern 1: UPI:515314436916
    const upiRefPattern = /UPI:(\d+)/i
    let match = message.match(upiRefPattern)
    if (match) {
      return match[1]
    }

    // Pattern 2: Ref No. 123456
    const refNoPattern = /Ref\s+No\.?\s*(\w+)/i
    match = message.match(refNoPattern)
    if (match) {
      return match[1]
    }

    // Pattern 3: Transaction ID: ABC123
    const txnIdPattern = /Transaction\s+ID:?\s*(\w+)/i
    match = message.match(txnIdPattern)
    if (match) {
      return match[1]
    }

    // Fall back to base class
    return super.extractReference(message)
  }

  protected extractBalance(message: string): number | null {
    // Pattern 1: Bal Rs. 50000.00
    const balPattern1 = /Bal\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    let match = message.match(balPattern1)
    if (match) {
      const balanceStr = match[1].replace(/,/g, '')
      const balance = parseFloat(balanceStr)
      if (!isNaN(balance)) {
        return balance
      }
    }

    // Pattern 2: Available Balance: Rs. 25000
    const balPattern2 = /Available\s+Balance:?\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    match = message.match(balPattern2)
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

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()

    // Indian Bank specific patterns
    if (lowerMessage.includes('debited')) return TransactionType.EXPENSE
    if (lowerMessage.includes('withdrawn')) return TransactionType.EXPENSE
    if (lowerMessage.includes('upi payment') && !lowerMessage.includes('received')) return TransactionType.EXPENSE

    if (lowerMessage.includes('credited')) return TransactionType.INCOME
    if (lowerMessage.includes('deposited')) return TransactionType.INCOME
    if (lowerMessage.includes('received')) return TransactionType.INCOME

    // Fall back to base class for other patterns
    return super.extractTransactionType(message)
  }

  /**
   * Checks if the message is a mandate notification
   * Example: "For the upcoming mandate set for 29-May-25 ,your account will be debited with INR 59.00 towards Spotify India ."
   */
  isMandateNotification(message: string): boolean {
    const lowerMessage = message.toLowerCase()
    return lowerMessage.includes('mandate') && 
           (lowerMessage.includes('upcoming') || 
            lowerMessage.includes('set for') || 
            lowerMessage.includes('will be debited'))
  }

  /**
   * Parses mandate/subscription information from the message
   */
  parseMandateSubscription(message: string): MandateInfo | null {
    // Pattern: "For the upcoming mandate set for 29-May-25 ,your account will be debited with INR 59.00 towards Spotify India ."
    const mandatePattern = /mandate\s+set\s+for\s+(\d{1,2}-\w{3}-\d{2})\s*,?\s*your\s+account\s+will\s+be\s+debited\s+with\s+INR\s+(\d+(?:\.\d{2})?)\s+towards\s+([^.]+)/i
    
    const match = message.match(mandatePattern)
    if (match) {
      const dateStr = match[1]  // e.g., "29-May-25"
      const amount = parseFloat(match[2])
      const merchant = this.cleanMerchantName(match[3].trim())

      return {
        amount,
        nextDeductionDate: dateStr,
        merchant
      }
    }

    return null
  }

  protected isTransactionMessage(message: string): boolean {
    // Skip mandate notifications
    if (this.isMandateNotification(message)) {
      return false
    }

    // Use base class logic for other checks
    return super.isTransactionMessage(message)
  }
}