import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const SBIConfig: BankConfig = {
  bankName: 'State Bank of India',
  bankCode: 'sbi',
  senders: {
    exact: ['SBIBK', 'SBIBNK', 'SBI', 'ATMSBI', 'SBIINB', 'SBIUPI', 'SBICRD'],
    contains: ['SBI'],
    patterns: [
      /^[A-Z]{2}-SBIBK-S$/,
      /^[A-Z]{2}-SBIBK-[TPG]$/,
      /^[A-Z]{2}-SBIBK$/,
      /^[A-Z]{2}-SBI$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'spent', 'charged', 'paid', 'purchase', 'transferred', 'paid to'],
    income: ['credited', 'deposited', 'received', 'refund']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount',
    'e-statement of sbi credit card'
  ]
}

export class SBIBankParser extends BankParser {
  constructor() {
    super(SBIConfig)
  }

  protected extractAmount(message: string): number | null {
    // SBI specific patterns
    const sbiPatterns = [
      // Pattern 0: A/c debited by 20.0 (UPI format)
      /debited\s+by\s+(\d+(?:,\d{3})*(?:\.\d{1,2})?)/i,
      // Pattern 1: Rs 500 debited
      /Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?debited/i,
      // Pattern 2: INR 500 debited
      /INR\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?debited/i,
      // Pattern 3: Rs 500 credited
      /Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?credited/i,
      // Pattern 4: INR 500 credited
      /INR\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?credited/i,
      // Pattern 5: withdrawn Rs 500
      /withdrawn\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i,
      // Pattern 6: transferred Rs 500
      /transferred\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i,
      // Pattern 7: UPI patterns - "paid to MERCHANT@upi Rs 500"
      /paid\s+to\s+[\w.-]+@[\w]+\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i,
      // Pattern 8: ATM withdrawal - "ATM withdrawal of Rs 500"
      /ATM\s+withdrawal\s+of\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i,
      // Pattern 9: YONO Cash withdrawal - "Yono Cash Rs 3000 w/d@SBI ATM"
      /Yono\s+Cash\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    ]

    for (const pattern of sbiPatterns) {
      const match = message.match(pattern)
      if (match) {
        const amountStr = match[1].replace(/,/g, '')
        const amount = parseFloat(amountStr)
        if (!isNaN(amount)) {
          return amount
        }
      }
    }

    // Fall back to base class patterns
    return super.extractAmount(message)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()

    // SBI-specific patterns
    if (lowerMessage.includes('withdrawn')) {
      return TransactionType.EXPENSE
    }
    if (lowerMessage.includes('transferred')) {
      return TransactionType.EXPENSE
    }
    if (lowerMessage.includes('paid to')) {
      return TransactionType.EXPENSE
    }
    if (lowerMessage.includes('atm withdrawal')) {
      return TransactionType.EXPENSE
    }

    // Fall back to base class for common patterns
    return super.extractTransactionType(message)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Pattern 0: trf to Mrs Shopkeeper (UPI format)
    const trfPattern = /trf\s+to\s+([^.\n]+?)(?:\s+Ref|\s+ref|$)/i
    const trfMatch = message.match(trfPattern)
    if (trfMatch) {
      const merchant = this.cleanMerchantName(trfMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 1: paid to MERCHANT@upi
    const upiMerchantPattern = /paid\s+to\s+([\w.-]+)@[\w]+/i
    const upiMatch = message.match(upiMerchantPattern)
    if (upiMatch) {
      const merchant = this.cleanMerchantName(upiMatch[1])
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 2: YONO Cash ATM - "w/d@SBI ATM S1NW000093009"
    const yonoAtmPattern = /w\/d@SBI\s+ATM\s+([A-Z0-9]+)/i
    const yonoMatch = message.match(yonoAtmPattern)
    if (yonoMatch) {
      return `YONO Cash ATM - ${yonoMatch[1]}`
    }

    // Pattern 2a: Regular ATM location
    const atmPattern = /ATM\s+(?:withdrawal\s+)?(?:at\s+)?([^.\n]+?)(?:\s+on|\s+Avl)/i
    const atmMatch = message.match(atmPattern)
    if (atmMatch) {
      const location = this.cleanMerchantName(atmMatch[1])
      if (this.isValidMerchantName(location)) {
        return `ATM - ${location}`
      }
    }

    // Pattern 3: NEFT/IMPS/RTGS with beneficiary
    const neftPattern = /(?:NEFT|IMPS|RTGS)[^:]*:\s*([^.\n]+?)(?:\s+Ref|\s+on|$)/i
    const neftMatch = message.match(neftPattern)
    if (neftMatch) {
      const merchant = this.cleanMerchantName(neftMatch[1])
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Fall back to base class patterns
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // SBI specific patterns
    const sbiPatterns = [
      // Pattern 1: A/c XX1234
      /A\/c\s+(?:XX|X\*+)?(\d{4})/i,
      // Pattern 2: from A/c ending 1234
      /A\/c\s+ending\s+(\d{4})/i,
      // Pattern 3: a/c no. XX1234
      /a\/c\s+no\.?\s+(?:XX|X\*+)?(\d{4})/i
    ]

    for (const pattern of sbiPatterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1]
      }
    }

    // Fall back to base class
    return super.extractAccountLast4(message)
  }

  protected extractBalance(message: string): number | null {
    // SBI specific patterns
    const sbiPatterns = [
      // Pattern 1: Avl Bal Rs 1000.00
      /Avl\s+Bal\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i,
      // Pattern 2: Available Balance: Rs 1000
      /Available\s+Balance:?\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i,
      // Pattern 3: Bal: Rs 1000
      /Bal:?\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    ]

    for (const pattern of sbiPatterns) {
      const match = message.match(pattern)
      if (match) {
        const balanceStr = match[1].replace(/,/g, '')
        const balance = parseFloat(balanceStr)
        if (!isNaN(balance)) {
          return balance
        }
      }
    }

    // Fall back to base class
    return super.extractBalance(message)
  }

  protected extractReference(message: string): string | null {
    // SBI specific patterns
    const sbiPatterns = [
      // Pattern 1: Ref No 123456789
      /Ref\s+No\.?\s*(\w+)/i,
      // Pattern 2: Txn# 123456
      /Txn#\s*(\w+)/i,
      // Pattern 3: transaction ID 123456
      /transaction\s+ID:?\s*(\w+)/i
    ]

    for (const pattern of sbiPatterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1]
      }
    }

    // Fall back to base class
    return super.extractReference(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Skip e-statement notifications
    if (lowerMessage.includes('e-statement of sbi credit card')) {
      return false
    }

    // Fall back to base class for other checks
    return super.isTransactionMessage(message)
  }
}