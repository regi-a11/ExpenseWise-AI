import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const ICICIConfig: BankConfig = {
  bankName: 'ICICI Bank',
  bankCode: 'icici',
  senders: {
    exact: ['ICICIB', 'ICICIBANK'],
    contains: ['ICICI', 'ICICIB'],
    patterns: [
      /^[A-Z]{2}-ICICIB-S$/,
      /^[A-Z]{2}-ICICI-S$/,
      /^[A-Z]{2}-ICICIB-[TPG]$/,
      /^[A-Z]{2}-ICICIB$/,
      /^[A-Z]{2}-ICICI$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'spent', 'charged', 'paid', 'purchase', 'autopay'],
    income: ['credited', 'deposited', 'received', 'refund', 'cashback']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'icici bank credit card'
  ]
}

export class ICICIBankParser extends BankParser {
  constructor() {
    super(ICICIConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: "debited with Rs xxx.00"
    const debitWithPattern = /debited\s+with\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)/i
    let match = message.match(debitWithPattern)
    if (match) {
      const amountStr = match[1].replace(/,/g, '')
      const amount = parseFloat(amountStr)
      if (!isNaN(amount)) return amount
    }

    // Pattern 2: "debited for Rs xxx.00"
    const debitForPattern = /debited\s+for\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)/i
    match = message.match(debitForPattern)
    if (match) {
      const amountStr = match[1].replace(/,/g, '')
      const amount = parseFloat(amountStr)
      if (!isNaN(amount)) return amount
    }

    // Pattern 3: "credited with Rs xxx.00"
    const creditWithPattern = /credited\s+with\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)/i
    match = message.match(creditWithPattern)
    if (match) {
      const amountStr = match[1].replace(/,/g, '')
      const amount = parseFloat(amountStr)
      if (!isNaN(amount)) return amount
    }

    // Pattern 4: "Cash deposit transaction of Rs xxx" (must have "has been completed")
    if (message.toLowerCase().includes('cash deposit transaction') && 
        message.toLowerCase().includes('has been completed')) {
      const cashDepositPattern = /Cash\s+deposit\s+transaction\s+of\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)/i
      match = message.match(cashDepositPattern)
      if (match) {
        const amountStr = match[1].replace(/,/g, '')
        const amount = parseFloat(amountStr)
        if (!isNaN(amount)) return amount
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

    // Pattern 2: "from <name>. UPI"
    const fromUpiPattern = /from\s+([^.\n]+?)\.\s*UPI/i
    match = message.match(fromUpiPattern)
    if (match) {
      const merchant = this.cleanMerchantName(match[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 3: "; <name> credited. UPI"
    const creditedPattern = /;\s*([^.\n]+?)\s+credited\.\s*UPI/i
    match = message.match(creditedPattern)
    if (match) {
      const merchant = this.cleanMerchantName(match[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 4: Cash deposit (only if completed)
    if (message.toLowerCase().includes('cash deposit transaction') && 
        message.toLowerCase().includes('has been completed')) {
      return 'Cash Deposit'
    }

    // Pattern 5: AutoPay specific - extract service name
    if (message.toLowerCase().includes('autopay')) {
      const lowerMessage = message.toLowerCase()
      if (lowerMessage.includes('google play')) return 'Google Play Store'
      if (lowerMessage.includes('netflix')) return 'Netflix'
      if (lowerMessage.includes('spotify')) return 'Spotify'
      if (lowerMessage.includes('amazon prime')) return 'Amazon Prime'
      if (lowerMessage.includes('disney') || lowerMessage.includes('hotstar')) return 'Disney+ Hotstar'
      if (lowerMessage.includes('youtube')) return 'YouTube Premium'
      return 'AutoPay Subscription'
    }

    // Fall back to base class patterns
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern 1: "Acct XXxxx"
    const acctPattern = /Acct\s+(?:XX|X\*+)?(\d{3,4})/i
    let match = message.match(acctPattern)
    if (match) {
      return match[1]
    }

    // Pattern 2: "ICICI Bank Acct XXxxx"
    const bankAcctPattern = /ICICI\s+Bank\s+Acct\s+(?:XX|X\*+)?(\d{3,4})/i
    match = message.match(bankAcctPattern)
    if (match) {
      return match[1]
    }

    // Pattern 3: "ICICI Bank Account 1234XXXX1234" - extract last 4 visible digits
    const accountFullPattern = /ICICI\s+Bank\s+Account\s+\d+X+(\d{4})/i
    match = message.match(accountFullPattern)
    if (match) {
      return match[1]
    }

    // Fall back to base class
    return super.extractAccountLast4(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern 1: "RRN 1xxxxx3xxxxx"
    const rrnPattern = /RRN\s+([A-Za-z0-9]+)/i
    let match = message.match(rrnPattern)
    if (match) {
      return match[1]
    }

    // Pattern 2: "UPI:5xxxxx8xxxxx"
    const upiPattern = /UPI:([A-Za-z0-9]+)/i
    match = message.match(upiPattern)
    if (match) {
      return match[1]
    }

    // Pattern 3: "transaction reference no.MCDA001746000000"
    const txnRefPattern = /transaction\s+reference\s+no\.?([A-Z0-9]+)/i
    match = message.match(txnRefPattern)
    if (match) {
      return match[1]
    }

    // Fall back to base class
    return super.extractReference(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Skip credit card related messages
    if (lowerMessage.includes('icici bank credit card')) {
      return false
    }

    // Check for ICICI-specific transaction keywords
    const iciciKeywords = [
      'debited with',
      'debited for',
      'credited with',
      'autopay',
      'your account has been'
    ]

    // Special check for cash deposit - must be completed
    if (lowerMessage.includes('cash deposit transaction') && 
        lowerMessage.includes('has been completed')) {
      return true
    }

    // If any ICICI-specific pattern is found, it's likely a transaction
    if (iciciKeywords.some(keyword => lowerMessage.includes(keyword))) {
      return true
    }

    // Fall back to base class for standard checks
    return super.isTransactionMessage(message)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()

    // Cash deposit is income (only if completed)
    if (lowerMessage.includes('cash deposit transaction') && 
        lowerMessage.includes('has been completed')) {
      return TransactionType.INCOME
    }

    // Fall back to base class for standard checks
    return super.extractTransactionType(message)
  }
}