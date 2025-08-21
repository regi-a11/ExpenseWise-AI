import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const SliceConfig: BankConfig = {
  bankName: 'Slice',
  bankCode: 'slice',
  senders: {
    exact: ['SLICEIT', 'JK-SLICEIT', 'SLICE'],
    contains: ['SLICE', 'SLICEIT'],
    patterns: [
      /^[A-Z]{2}-SLICE.*$/,
      /^JK-SLICEIT$/
    ]
  },
  transactionType: {
    expense: ['debited', 'spent', 'paid', 'payment'],
    income: ['credited', 'received', 'cashback', 'refund']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class SliceParser extends BankParser {
  constructor() {
    super(SliceConfig)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    const lowerMessage = message.toLowerCase()
    
    // Look for "from MERCHANT" pattern
    const fromPattern = /from\s+([A-Z][A-Z0-9\s]+?)(?:\s+on|\s+\(|$)/i
    const fromMatch = message.match(fromPattern)
    if (fromMatch) {
      const merchant = fromMatch[1].trim()
      if (merchant && !merchant.match(/^NEFT$/i)) {
        return this.cleanMerchantName(merchant)
      }
    }
    
    // Check for specific patterns
    if (lowerMessage.includes('paypal')) return 'PayPal'
    if (lowerMessage.includes('slice') && lowerMessage.includes('credited')) return 'Slice Credit'
    
    // Try parent parser
    const parentMerchant = super.extractMerchant(message, sender)
    return parentMerchant || 'Slice'
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()
    
    // Slice credits/cashbacks
    if (lowerMessage.includes('credited')) return TransactionType.INCOME
    if (lowerMessage.includes('received')) return TransactionType.INCOME
    if (lowerMessage.includes('cashback')) return TransactionType.INCOME
    if (lowerMessage.includes('refund')) return TransactionType.INCOME
    
    // Slice payments/debits
    if (lowerMessage.includes('debited')) return TransactionType.EXPENSE
    if (lowerMessage.includes('spent')) return TransactionType.EXPENSE
    if (lowerMessage.includes('paid')) return TransactionType.EXPENSE
    if (lowerMessage.includes('payment') && !lowerMessage.includes('received')) return TransactionType.EXPENSE
    
    return super.extractTransactionType(message)
  }
}