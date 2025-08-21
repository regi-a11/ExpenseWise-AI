import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const JuspayConfig: BankConfig = {
  bankName: 'Amazon Pay',
  bankCode: 'amazonpay',
  senders: {
    exact: [],
    contains: ['JUSPAY'],
    patterns: []
  },
  transactionType: {
    expense: ['debited', 'paid', 'sent', 'charged'],
    income: ['credited', 'received', 'added']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class JuspayParser extends BankParser {
  constructor() {
    super(JuspayConfig)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // For Juspay/Amazon Pay, the merchant info is usually not in the message
    // These are typically payment gateway transactions
    
    // Check for common patterns
    const lowerMessage = message.toLowerCase()
    if (lowerMessage.includes('amazon')) return 'Amazon'
    if (lowerMessage.includes('apay wallet')) return 'Amazon Pay Transaction'
    if (lowerMessage.includes('wallet')) return 'Amazon Pay Transaction'

    return super.extractMerchant(message, sender) || 'Amazon Pay'
  }
}