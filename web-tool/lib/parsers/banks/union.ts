import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const UnionConfig: BankConfig = {
  bankName: 'Union Bank of India',
  bankCode: 'union',
  senders: {
    exact: ['UNIONB', 'UNIONBANK', 'UBOI'],
    contains: ['UNIONB', 'UNIONBANK', 'UBOI'],
    patterns: [
      /^[A-Z]{2}-UNIONB-[ST]$/,
      /^[A-Z]{2}-UNIONB-[TPG]$/,
      /^[A-Z]{2}-UNIONB$/,
      /^[A-Z]{2}-UNIONBANK$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'spent', 'paid', 'purchase'],
    income: ['credited', 'deposited', 'received', 'refund']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class UnionBankParser extends BankParser {
  constructor() {
    super(UnionConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: "Rs:100.00" or "Rs.100.00" (Union Bank format with colon)
    const amountPattern1 = /Rs[:.]?\s*([0-9,]+(?:\.\d{2})?)/i
    const amount1Match = message.match(amountPattern1)
    if (amount1Match) {
      const amount = amount1Match[1].replace(/,/g, '')
      const parsed = parseFloat(amount)
      return isNaN(parsed) ? null : parsed
    }
    
    // Pattern 2: "INR 500" format
    const amountPattern2 = /INR\s+([0-9,]+(?:\.\d{2})?)/i
    const amount2Match = message.match(amountPattern2)
    if (amount2Match) {
      const amount = amount2Match[1].replace(/,/g, '')
      const parsed = parseFloat(amount)
      return isNaN(parsed) ? null : parsed
    }
    
    // Fall back to base class patterns
    return super.extractAmount(message)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Pattern 1: Mobile Banking - "by Mob Bk"
    if (message.match(/Mob Bk/i)) {
      return 'Mobile Banking Transfer'
    }
    
    // Pattern 2: ATM transactions
    if (message.match(/ATM/i)) {
      const atmPattern = /at\s+([^.\s]+(?:\s+[^.\s]+)*)(?:\s+on|\s+Avl|$)/i
      const atmMatch = message.match(atmPattern)
      if (atmMatch) {
        return this.cleanMerchantName(atmMatch[1].trim())
      }
      return 'ATM Withdrawal'
    }
    
    // Pattern 3: UPI transactions - "UPI/merchant" or "VPA merchant@bank"
    if (message.match(/UPI/i)) {
      const upiPattern = /UPI[/:]?\s*([^,.\s]+)/i
      const upiMatch = message.match(upiPattern)
      if (upiMatch) {
        return this.cleanMerchantName(upiMatch[1].trim())
      }
    }
    
    if (message.match(/VPA/i)) {
      const vpaPattern = /VPA\s+([^@\s]+)/i
      const vpaMatch = message.match(vpaPattern)
      if (vpaMatch) {
        const vpaName = vpaMatch[1].trim()
        return this.parseUPIMerchant(vpaName)
      }
    }
    
    // Pattern 4: "to <merchant>" for transfers
    const toPattern = /to\s+([^.\n]+?)(?:\s+on|\s+Avl|$)/i
    const toMatch = message.match(toPattern)
    if (toMatch) {
      const merchant = toMatch[1].trim()
      if (!merchant.match(/Avl/i)) {
        return this.cleanMerchantName(merchant)
      }
    }
    
    // Pattern 5: "from <sender>" for credits
    const fromPattern = /from\s+([^.\n]+?)(?:\s+on|\s+Avl|$)/i
    const fromMatch = message.match(fromPattern)
    if (fromMatch) {
      const merchant = fromMatch[1].trim()
      if (!merchant.match(/Avl/i)) {
        return this.cleanMerchantName(merchant)
      }
    }
    
    // Fall back to base class extraction
    return super.extractMerchant(message, sender)
  }

  protected extractReference(message: string): string | null {
    // Union Bank format: "ref no 123456789000"
    const refPatterns = [
      /ref\s+no\s+([\w]+)/i,
      /ref[:#]?\s*([\w]+)/i,
      /reference[:#]?\s*([\w]+)/i,
      /txn[:#]?\s*([\w]+)/i
    ]
    
    for (const pattern of refPatterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1].trim()
      }
    }
    
    return super.extractReference(message)
  }

  protected extractAccountLast4(message: string): string | null {
    // Union Bank format: "A/c *1234" or "A/C X1234"
    const accountPatterns = [
      /A\/[Cc]\s*[*X](\d{4})/i,
      /Account\s*[*X](\d{4})/i,
      /Acc\s*[*X](\d{4})/i,
      /A\/[Cc]\s+(\d{4})/i
    ]
    
    for (const pattern of accountPatterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1]
      }
    }
    
    return super.extractAccountLast4(message)
  }

  protected extractBalance(message: string): number | null {
    // Union Bank format: "Avl Bal Rs:12345.67" or "Avl Bal Rs.12345.67"
    const balancePatterns = [
      /Avl\s+Bal\s+Rs[:.]?\s*([0-9,]+(?:\.\d{2})?)/i,
      /Available\s+Balance[:.]?\s*Rs[:.]?\s*([0-9,]+(?:\.\d{2})?)/i,
      /Balance[:.]?\s*Rs[:.]?\s*([0-9,]+(?:\.\d{2})?)/i,
      /Bal[:.]?\s*Rs[:.]?\s*([0-9,]+(?:\.\d{2})?)/i
    ]
    
    for (const pattern of balancePatterns) {
      const match = message.match(pattern)
      if (match) {
        const balanceStr = match[1].replace(/,/g, '')
        const parsed = parseFloat(balanceStr)
        return isNaN(parsed) ? null : parsed
      }
    }
    
    return super.extractBalance(message)
  }

  private parseUPIMerchant(vpa: string): string {
    const cleanVPA = vpa.toLowerCase()
    
    // Common payment apps and merchants
    if (cleanVPA.includes('paytm')) return 'Paytm'
    if (cleanVPA.includes('phonepe')) return 'PhonePe'
    if (cleanVPA.includes('googlepay') || cleanVPA.includes('gpay')) return 'Google Pay'
    if (cleanVPA.includes('bharatpe')) return 'BharatPe'
    if (cleanVPA.includes('amazon')) return 'Amazon'
    if (cleanVPA.includes('flipkart')) return 'Flipkart'
    if (cleanVPA.includes('swiggy')) return 'Swiggy'
    if (cleanVPA.includes('zomato')) return 'Zomato'
    if (cleanVPA.includes('uber')) return 'Uber'
    if (cleanVPA.includes('ola')) return 'Ola'
    
    // Individual transfers (just numbers)
    if (cleanVPA.match(/^\d+$/)) return 'Individual'
    
    // Default - clean up the VPA name
    const parts = cleanVPA.split(/[.\-_]/)
    const meaningfulPart = parts.find(part => part.length > 3 && !part.match(/^\d+$/))
    if (meaningfulPart) {
      return meaningfulPart.charAt(0).toUpperCase() + meaningfulPart.slice(1)
    }
    
    return 'Merchant'
  }
}