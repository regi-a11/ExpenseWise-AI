import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const HDFCConfig: BankConfig = {
  bankName: 'HDFC Bank',
  bankCode: 'hdfc',
  senders: {
    exact: ['HDFCBK', 'HDFCBANK', 'HDFC', 'HDFCB'],
    contains: ['HDFC'],
    patterns: [
      /^[A-Z]{2}-HDFCBK.*$/,
      /^[A-Z]{2}-HDFC.*$/,
      /^HDFC-[A-Z]+$/,
      /^[A-Z]{2}-HDFCB.*$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'spent', 'charged', 'paid', 'purchase', 'sent', 'deducted', 'txn'],
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
    'e-mandate!',
    'block cc',
    'received towards your credit card',
    'credited to your card'
  ]
}

export class HDFCBankParser extends BankParser {
  constructor() {
    super(HDFCConfig)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Try HDFC specific patterns first

    // Pattern 1: Salary credit - "for XXXXX-ABC-XYZ MONTH SALARY-COMPANY NAME"
    if (message.toLowerCase().includes('salary') && message.toLowerCase().includes('deposited')) {
      const salaryPattern = /for\s+[^-]+-[^-]+-[^-]+\s+[A-Z]+\s+SALARY-([^.\n]+)/i
      const match = message.match(salaryPattern)
      if (match) {
        return this.cleanMerchantName(match[1].trim())
      }

      // Simpler salary pattern
      const simpleSalaryPattern = /SALARY[- ]([^.\n]+?)(?:\s+Info|$)/i
      const simpleMatch = message.match(simpleSalaryPattern)
      if (simpleMatch) {
        const merchant = simpleMatch[1].trim()
        if (merchant && !/^\d+$/.test(merchant)) {
          return this.cleanMerchantName(merchant)
        }
      }
    }

    // Pattern 2: "Info: UPI/merchant/category" format
    if (message.toLowerCase().includes('info:')) {
      const infoPattern = /Info:\s*(?:UPI\/)?([^\/.\n]+?)(?:\/|$)/i
      const match = message.match(infoPattern)
      if (match) {
        const merchant = match[1].trim()
        if (merchant && merchant.toUpperCase() !== 'UPI') {
          return this.cleanMerchantName(merchant)
        }
      }
    }

    // Pattern 3: "VPA merchant@bank (Merchant Name)" format
    if (message.toLowerCase().includes('vpa')) {
      // First try to get name in parentheses
      const vpaWithNamePattern = /VPA\s+[^@\s]+@[^\s]+\s*\(([^)]+)\)/i
      const nameMatch = message.match(vpaWithNamePattern)
      if (nameMatch) {
        return this.cleanMerchantName(nameMatch[1].trim())
      }

      // Then try just the VPA username part
      const vpaPattern = /VPA\s+([^@\s]+)@/i
      const vpaMatch = message.match(vpaPattern)
      if (vpaMatch) {
        const vpaName = vpaMatch[1].trim()
        if (vpaName.length > 3 && !/^\d+$/.test(vpaName)) {
          return this.cleanMerchantName(vpaName)
        }
      }
    }

    // Pattern 4: "spent on Card XX1234 at merchant on date"
    if (message.toLowerCase().includes('spent on card')) {
      const spentPattern = /at\s+([^.\n]+?)\s+on\s+\d{2}/i
      const match = message.match(spentPattern)
      if (match) {
        return this.cleanMerchantName(match[1].trim())
      }
    }

    // Pattern 5: "debited for merchant on date"
    if (message.toLowerCase().includes('debited for')) {
      const debitForPattern = /debited\s+for\s+([^.\n]+?)\s+on\s+\d{2}/i
      const match = message.match(debitForPattern)
      if (match) {
        return this.cleanMerchantName(match[1].trim())
      }
    }

    // Pattern 6: "To merchant name" (for UPI mandate)
    if (message.toLowerCase().includes('upi mandate')) {
      const mandatePattern = /To\s+([^\n]+?)\s*(?:\n|\d{2}\/\d{2})/i
      const match = message.match(mandatePattern)
      if (match) {
        return this.cleanMerchantName(match[1].trim())
      }
    }

    // Fall back to base extraction
    return super.extractMerchant(message, sender)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()

    // HDFC specific: "Sent Rs.X From HDFC Bank"
    if (lowerMessage.includes('sent') && lowerMessage.includes('from hdfc')) {
      return TransactionType.EXPENSE
    }

    // Use base class logic for standard cases
    return super.extractTransactionType(message)
  }

  protected extractReference(message: string): string | null {
    // HDFC specific reference patterns
    const hdfcPatterns = [
      /Ref\s+(\d{9,12})/i,
      /UPI\s+Ref\s+No\s+(\d{12})/i,
      /Ref\s+No\.?\s+([A-Z0-9]+)/i,
      /(?:Ref|Reference)[:.\s]+([A-Z0-9]{6,})(?:\s*$|\s*Not\s+You)/i
    ]

    for (const pattern of hdfcPatterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1].trim()
      }
    }

    // Fall back to base extraction
    return super.extractReference(message)
  }

  protected extractAccountLast4(message: string): string | null {
    // HDFC specific patterns
    const hdfcPatterns = [
      /deposited\s+in\s+(?:HDFC\s+Bank\s+)?A\/c\s+(?:XX+)?(\d{4})/i,
      /from\s+(?:HDFC\s+Bank\s+)?A\/c\s+(?:XX+)?(\d{4})/i,
      /HDFC\s+Bank\s+A\/c\s+(\d{4})/i,
      /A\/c\s+(?:XX+)?(\d{4})/i
    ]

    for (const pattern of hdfcPatterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1]
      }
    }

    return super.extractAccountLast4(message)
  }

  /**
   * Check if this is an E-Mandate notification (not a transaction)
   */
  isEMandateNotification(message: string): boolean {
    return message.toLowerCase().includes('e-mandate!')
  }

  protected isTransactionMessage(message: string): boolean {
    // Skip E-Mandate notifications
    if (this.isEMandateNotification(message)) {
      return false
    }

    const lowerMessage = message.toLowerCase()

    // Skip future transaction notifications (reminders)
    if (lowerMessage.includes('will be')) {
      return false
    }

    // Skip credit card blocking notifications
    if (lowerMessage.includes('block cc')) {
      return false
    }

    // Skip credit card payment confirmations
    if (lowerMessage.includes('received towards your credit card')) {
      return false
    }

    // Skip credit card payment credited notifications
    if (lowerMessage.includes('payment') && lowerMessage.includes('credited to your card')) {
      return false
    }

    // Use base class logic for other checks
    return super.isTransactionMessage(message)
  }
}