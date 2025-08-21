import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const KotakConfig: BankConfig = {
  bankName: 'Kotak Bank',
  bankCode: 'kotak',
  senders: {
    exact: ['KOTAKB', 'KOTAK'],
    contains: ['KOTAK'],
    patterns: [
      /^[A-Z]{2}-KOTAKB-[ST]$/,
      /^[A-Z]{2}-KOTAK.*$/
    ]
  },
  transactionType: {
    expense: ['sent', 'debited', 'withdrawn', 'spent', 'charged', 'paid', 'purchase'],
    income: ['credited', 'deposited', 'received', 'refund', 'cashback']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount',
    'cashback offer',
    'win ',
    'has requested',
    'payment request',
    'collect request',
    'requesting payment',
    'requests rs',
    'ignore if already paid'
  ]
}

export class KotakBankParser extends BankParser {
  constructor() {
    super(KotakConfig)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Pattern 1: "Sent Rs.X from Kotak Bank AC XXXX to merchant@bank on"
    // Extract merchant from UPI ID like "upiswiggy@icici" or "amazonpayrecharges@apl"
    const toPattern = /to\s+([^\s]+@[^\s]+)\s+on/i
    const toMatch = message.match(toPattern)
    if (toMatch) {
      const upiId = toMatch[1].trim()
      
      // Extract merchant name from UPI ID
      let merchantName: string | null = null
      
      if (upiId.toLowerCase().startsWith('upi')) {
        // Handle "upiXXX@bank" format - remove "upi" prefix
        const name = upiId.substring(3).split('@')[0]
        if (name) {
          merchantName = this.cleanMerchantName(name)
        }
      } else {
        // Handle other UPI IDs - extract username part
        const name = upiId.split('@')[0]
        if (name && !name.match(/^\d+$/)) {
          merchantName = this.cleanMerchantName(name)
        }
      }
      
      if (merchantName && this.isValidMerchantName(merchantName)) {
        return merchantName
      }
    }
    
    // Fall back to generic extraction
    return super.extractMerchant(message, sender)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()
    
    // Kotak specific: "Sent Rs.X from Kotak Bank"
    if (lowerMessage.includes('sent') && lowerMessage.includes('from kotak')) {
      return TransactionType.EXPENSE
    }
    
    // Standard expense keywords
    if (lowerMessage.includes('debited')) return TransactionType.EXPENSE
    if (lowerMessage.includes('withdrawn')) return TransactionType.EXPENSE
    if (lowerMessage.includes('spent')) return TransactionType.EXPENSE
    if (lowerMessage.includes('charged')) return TransactionType.EXPENSE
    if (lowerMessage.includes('paid')) return TransactionType.EXPENSE
    if (lowerMessage.includes('purchase')) return TransactionType.EXPENSE
    
    // Income keywords
    if (lowerMessage.includes('credited')) return TransactionType.INCOME
    if (lowerMessage.includes('deposited')) return TransactionType.INCOME
    if (lowerMessage.includes('received')) return TransactionType.INCOME
    if (lowerMessage.includes('refund')) return TransactionType.INCOME
    if (lowerMessage.includes('cashback') && !lowerMessage.includes('earn cashback')) {
      return TransactionType.INCOME
    }
    
    return null
  }

  protected extractReference(message: string): string | null {
    // Kotak specific UPI reference pattern
    const upiRefPattern = /UPI\s+Ref\s+([0-9]+)/i
    const upiRefMatch = message.match(upiRefPattern)
    if (upiRefMatch) {
      return upiRefMatch[1].trim()
    }
    
    // Fall back to generic extraction
    return super.extractReference(message)
  }

  protected extractAccountLast4(message: string): string | null {
    // Kotak specific pattern: "AC X0000" or "AC XXXX0000"
    const kotakAccountPattern = /AC\s+[X*]*([0-9]{4})(?:\s|,|\.)/i
    const kotakAccountMatch = message.match(kotakAccountPattern)
    if (kotakAccountMatch) {
      return kotakAccountMatch[1]
    }
    
    return super.extractAccountLast4(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()
    
    // Skip fraud warning links but still process the transaction
    // Skip OTP and promotional messages
    if (lowerMessage.includes('otp') || 
        lowerMessage.includes('one time password') ||
        lowerMessage.includes('verification code') ||
        lowerMessage.includes('offer') || 
        lowerMessage.includes('discount') ||
        lowerMessage.includes('cashback offer') ||
        lowerMessage.includes('win ')) {
      return false
    }
    
    // Skip payment request messages
    if (lowerMessage.includes('has requested') || 
        lowerMessage.includes('payment request') ||
        lowerMessage.includes('collect request') ||
        lowerMessage.includes('requesting payment') ||
        lowerMessage.includes('requests rs') ||
        lowerMessage.includes('ignore if already paid')) {
      return false
    }
    
    // Kotak specific transaction keywords
    const kotakTransactionKeywords = [
      'sent', // Kotak uses "Sent Rs.X from Kotak Bank"
      'debited', 'credited', 'withdrawn', 'deposited',
      'spent', 'received', 'transferred', 'paid'
    ]
    
    return kotakTransactionKeywords.some(keyword => lowerMessage.includes(keyword))
  }

  protected isValidMerchantName(name: string): boolean {
    // Validate merchant name
    return name.length > 0 && name.length < 100 && !name.match(/^[0-9]+$/)
  }
}