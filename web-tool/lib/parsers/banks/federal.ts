import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const FederalBankConfig: BankConfig = {
  bankName: 'Federal Bank',
  bankCode: 'federal',
  senders: {
    exact: ['FEDBNK', 'FEDERAL', 'FEDFIB'],
    contains: ['FEDBNK', 'FEDERAL', 'FEDFIB'],
    patterns: [
      /^[A-Z]{2}-FEDBNK-S$/,
      /^[A-Z]{2}-FedFiB-[A-Z]$/,
      /^[A-Z]{2}-FEDBNK-[TPG]$/,
      /^[A-Z]{2}-FEDBNK$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'sent', 'paid', 'purchase'],
    income: ['credited', 'deposited', 'received']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class FederalBankParser extends BankParser {
  constructor() {
    super(FederalBankConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: Rs 34.51 debited via UPI
    const debitPattern = /Rs\s+([0-9,]+(?:\.\d{2})?)\s+debited/i
    let match = message.match(debitPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 2: Rs 500.00 credited
    const creditPattern = /Rs\s+([0-9,]+(?:\.\d{2})?)\s+credited/i
    match = message.match(creditPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 3: withdrawn Rs 500
    const withdrawnPattern = /withdrawn\s+Rs\s+([0-9,]+(?:\.\d{2})?)/i
    match = message.match(withdrawnPattern)
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
    // Pattern 1: UPI transactions - "to VPA merchant@bank"
    if (message.toLowerCase().includes('vpa')) {
      const vpaPattern = /to\s+VPA\s+([^.\s]+)/i
      const match = message.match(vpaPattern)
      if (match) {
        const vpa = match[1].trim()
        return this.parseUPIMerchant(vpa)
      }
    }

    // Pattern 2: "to <merchant name>" (general)
    const toPattern = /to\s+([^.\n]+?)(?:\.\s*Ref|Ref\s+No|$)/i
    const toMatch = message.match(toPattern)
    if (toMatch) {
      const merchant = toMatch[1].trim()
      // Skip if it's VPA (already handled above)
      if (!merchant.toLowerCase().includes('vpa')) {
        const cleaned = this.cleanMerchantName(merchant)
        if (this.isValidMerchantName(cleaned)) {
          return cleaned
        }
      }
    }

    // Pattern 3: "from <sender name>" (for credits)
    const fromPattern = /from\s+([^.\n]+?)(?:\.\s*|$)/i
    const fromMatch = message.match(fromPattern)
    if (fromMatch) {
      const merchant = this.cleanMerchantName(fromMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 4: ATM transactions
    if (message.toLowerCase().includes('atm') || 
        message.toLowerCase().includes('withdrawn')) {
      return 'ATM Withdrawal'
    }

    // Fall back to base class extraction
    return super.extractMerchant(message, sender)
  }

  private parseUPIMerchant(vpa: string): string {
    // Extract merchant name from VPA
    const cleanVPA = vpa.split('@')[0].toLowerCase()

    // Ride-hailing
    if (cleanVPA.includes('uber')) return 'Uber'
    if (cleanVPA.includes('ola')) return 'Ola'
    if (cleanVPA.includes('rapido')) return 'Rapido'

    // Payment apps
    if (cleanVPA.includes('paytm')) return 'Paytm'
    if (cleanVPA.includes('bharatpe')) return 'BharatPe'
    if (cleanVPA.includes('phonepe')) return 'PhonePe'
    if (cleanVPA.includes('googlepay') || cleanVPA.includes('gpay')) return 'Google Pay'

    // E-commerce
    if (cleanVPA.includes('amazon')) return 'Amazon'
    if (cleanVPA.includes('flipkart')) return 'Flipkart'
    if (cleanVPA.includes('myntra')) return 'Myntra'
    if (cleanVPA.includes('meesho')) return 'Meesho'

    // Food delivery
    if (cleanVPA.includes('swiggy')) return 'Swiggy'
    if (cleanVPA.includes('zomato')) return 'Zomato'

    // Entertainment
    if (cleanVPA.includes('netflix')) return 'Netflix'
    if (cleanVPA.includes('spotify')) return 'Spotify'
    if (cleanVPA.includes('hotstar') || cleanVPA.includes('disney')) return 'Disney+ Hotstar'
    if (cleanVPA.includes('prime')) return 'Amazon Prime'
    if (cleanVPA.includes('pvr') || cleanVPA.includes('inox')) return 'PVR Inox'
    if (cleanVPA.includes('bookmyshow') || cleanVPA.includes('bms')) return 'BookMyShow'

    // Telecom
    if (cleanVPA.includes('jio')) return 'Jio'
    if (cleanVPA.includes('airtel')) return 'Airtel'
    if (cleanVPA.includes('vodafone') || cleanVPA.includes('vi')) return 'Vi'
    if (cleanVPA.includes('bsnl')) return 'BSNL'

    // Travel
    if (cleanVPA.includes('irctc')) return 'IRCTC'
    if (cleanVPA.includes('redbus')) return 'RedBus'
    if (cleanVPA.includes('makemytrip') || cleanVPA.includes('mmt')) return 'MakeMyTrip'
    if (cleanVPA.includes('goibibo')) return 'Goibibo'
    if (cleanVPA.includes('oyo')) return 'OYO'
    if (cleanVPA.includes('airbnb')) return 'Airbnb'

    // Payment gateways - try to extract actual merchant
    if (cleanVPA.includes('razorpay') || cleanVPA.includes('razorp') || cleanVPA.includes('rzp')) {
      if (cleanVPA.includes('pvr')) return 'PVR'
      if (cleanVPA.includes('inox')) return 'PVR Inox'
      if (cleanVPA.includes('swiggy')) return 'Swiggy'
      if (cleanVPA.includes('zomato')) return 'Zomato'
      return 'Online Payment'
    }
    if (cleanVPA.includes('payu') || cleanVPA.includes('billdesk') || cleanVPA.includes('ccavenue')) {
      return 'Online Payment'
    }

    // Individual transfers (just numbers)
    if (/^\d+$/.test(cleanVPA)) return 'Individual'

    // Default - try to extract meaningful name
    const parts = cleanVPA.split(/[.\-_]/)
    const meaningfulPart = parts.find(part => part.length > 3 && !/^\d+$/.test(part))
    if (meaningfulPart) {
      return meaningfulPart.charAt(0).toUpperCase() + meaningfulPart.slice(1)
    }
    
    return 'Merchant'
  }
}