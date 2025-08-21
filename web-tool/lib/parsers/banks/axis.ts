import { BankParser } from '../base'
import { BankConfig } from '../types'

const AxisConfig: BankConfig = {
  bankName: 'Axis Bank',
  bankCode: 'axis',
  senders: {
    exact: ['AXISBK', 'AXISBANK', 'AXIS'],
    contains: ['AXIS BANK', 'AXISBANK', 'AXISBK', 'AXISB'],
    patterns: [
      /^[A-Z]{2}-AXISBK-S$/,
      /^[A-Z]{2}-AXISBANK-S$/,
      /^[A-Z]{2}-AXIS-S$/,
      /^[A-Z]{2}-AXISBK$/,
      /^[A-Z]{2}-AXIS$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'spent', 'charged', 'paid', 'purchase', 'payment'],
    income: ['credited', 'deposited', 'received', 'refund']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'has requested money',
    'will be debited',
    'on approval',
    'due',
    'overdue',
    'bill',
    'credit card'
  ]
}

export class AxisBankParser extends BankParser {
  constructor() {
    super(AxisConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: "INR xxx debited"
    const inrDebitPattern = /INR\s+([0-9,]+(?:\.\d{2})?)\s+debited/i
    let match = message.match(inrDebitPattern)
    if (match) {
      const amountStr = match[1].replace(/,/g, '')
      const amount = parseFloat(amountStr)
      if (!isNaN(amount)) return amount
    }

    // Pattern 2: "INR xxx credited"
    const inrCreditPattern = /INR\s+([0-9,]+(?:\.\d{2})?)\s+credited/i
    match = message.match(inrCreditPattern)
    if (match) {
      const amountStr = match[1].replace(/,/g, '')
      const amount = parseFloat(amountStr)
      if (!isNaN(amount)) return amount
    }

    // Pattern 3: "Payment of INR xxx"
    const paymentPattern = /Payment\s+of\s+INR\s+([0-9,]+(?:\.\d{2})?)/i
    match = message.match(paymentPattern)
    if (match) {
      const amountStr = match[1].replace(/,/g, '')
      const amount = parseFloat(amountStr)
      if (!isNaN(amount)) return amount
    }

    return super.extractAmount(message)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Pattern 1: "UPI/xxx/yyy/merchant"
    const upiMerchantPattern = /UPI\/[^\/]+\/[^\/]+\/([^\n]+?)(?:\s*Not you|\s*$)/i
    let match = message.match(upiMerchantPattern)
    if (match) {
      const merchant = this.cleanMerchantName(match[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 2: "UPI/P2A/xxx/person"
    const upiPersonPattern = /UPI\/P2A\/[^\/]+\/([^\n]+?)(?:\s*Not you|\s*$)/i
    match = message.match(upiPersonPattern)
    if (match) {
      const merchant = this.cleanMerchantName(match[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 3: "Info - merchant"
    const infoPattern = /Info\s*[-–]\s*([^.\n]+?)(?:\.\s*Chk|\s*$)/i
    match = message.match(infoPattern)
    if (match) {
      const info = match[1].trim()
      if (info.toLowerCase().includes('salary')) {
        return 'Salary'
      }
      return this.cleanMerchantName(info)
    }

    // Fall back to base class patterns
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern 1: "A/c no. XXxxxx"
    const acNoPattern = /A\/c\s+no\.\s+(?:XX|X\*+)?(\d{4,5})/i
    const match = message.match(acNoPattern)
    if (match) {
      const digits = match[1]
      return digits.length > 4 ? digits.slice(-4) : digits
    }

    // Pattern 2: "Credit Card XXxxxx"
    const creditCardPattern = /Credit\s+Card\s+(?:XX|X\*+)?(\d{4})/i
    const ccMatch = message.match(creditCardPattern)
    if (ccMatch) {
      return ccMatch[1]
    }

    return super.extractAccountLast4(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern: "UPI/xxx/123456789"
    const upiRefPattern = /UPI\/[^\/]+\/([0-9]+)/i
    const match = message.match(upiRefPattern)
    if (match) {
      return match[1]
    }

    return super.extractReference(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Skip credit card payment confirmation messages
    if (lowerMessage.includes('payment') && 
        lowerMessage.includes('has been received') && 
        lowerMessage.includes('towards your axis bank credit card')) {
      return false
    }

    // Skip UPI payment request messages (not actual transactions)
    if (lowerMessage.includes('has requested money') ||
        lowerMessage.includes('will be debited') ||
        lowerMessage.includes('on approval')) {
      return false
    }

    // Skip credit card bill due/overdue reminder messages
    if ((lowerMessage.includes(' due ') || lowerMessage.includes(' overdue')) &&
        (lowerMessage.includes('bill') || lowerMessage.includes('payment')) &&
        (lowerMessage.includes('credit card') || lowerMessage.includes(' cc '))) {
      return false
    }

    return super.isTransactionMessage(message)
  }
}