import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const UtkarshConfig: BankConfig = {
  bankName: 'Utkarsh Bank',
  bankCode: 'utkarsh',
  senders: {
    exact: ['UTKSPR', 'UTKSFB', 'UTKARSH'],
    contains: ['UTKSPR', 'UTKARSH', 'UTKSFB'],
    patterns: [
      /^[A-Z]{2}-UTKSPR$/,
      /^[A-Z]{2}-UTKSFB$/,
      /^[A-Z]{2}-UTKARSH$/
    ]
  },
  transactionType: {
    expense: ['debited', 'spent', 'paid', 'purchase', 'transaction'],
    income: ['credited', 'received', 'refund']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class UtkarshBankParser extends BankParser {
  constructor() {
    super(UtkarshConfig)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    const lowerMessage = message.toLowerCase()
    
    // Pattern 1: "for UPI - merchant/reference"
    const upiPattern = /for\s+UPI\s*[-–]\s*([^\s.]+)/i
    const upiMatch = message.match(upiPattern)
    if (upiMatch) {
      const merchant = upiMatch[1].trim()
      // Check if it's just a reference number (all digits or with x's)
      if (!merchant.match(/^[x0-9]+$/)) {
        return this.cleanMerchantName(merchant)
      }
    }
    
    // Pattern 2: "for merchant on date"
    const forPattern = /for\s+([^0-9][^\s]+?)(?:\s+on\s+|\s+at\s+|$)/i
    const forMatch = message.match(forPattern)
    if (forMatch) {
      const merchant = forMatch[1].trim()
      if (!merchant.match(/^(UPI|INR)$/i)) {
        return this.cleanMerchantName(merchant)
      }
    }
    
    // Check for specific patterns
    if (lowerMessage.includes('supercard') && lowerMessage.includes('upi')) {
      return 'UPI Payment'
    }
    
    // Try parent parser
    const parentMerchant = super.extractMerchant(message, sender)
    return parentMerchant || 'Utkarsh SuperCard'
  }

  protected extractTransactionType(message: string): TransactionType | null {
    // Utkarsh SuperCard is a credit card product, all transactions are credit (expenses)
    return TransactionType.EXPENSE
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern for SuperCard xxxx
    const cardPattern = /SuperCard\s+[xX*]*(\d{4})/i
    const cardMatch = message.match(cardPattern)
    if (cardMatch) {
      return cardMatch[1]
    }
    
    // Pattern for account XXXX
    const accountPattern = /(?:account|a\/c)\s+[xX*]*(\d{4})/i
    const accountMatch = message.match(accountPattern)
    if (accountMatch) {
      return accountMatch[1]
    }
    
    return super.extractAccountLast4(message)
  }
}