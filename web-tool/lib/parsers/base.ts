import { TransactionType, ParsedTransaction, BankConfig } from './types'
import { CategoryMapper } from '../categorization'

export abstract class BankParser {
  protected config: BankConfig

  constructor(config: BankConfig) {
    this.config = config
  }

  /**
   * Get the bank name
   */
  getBankName(): string {
    return this.config.bankName
  }

  /**
   * Get the bank code
   */
  getBankCode(): string {
    return this.config.bankCode
  }

  /**
   * Check if this parser can handle messages from the given sender
   */
  canHandle(sender: string): boolean {
    const upperSender = sender.toUpperCase()
    
    // Check exact matches
    if (this.config.senders.exact.includes(upperSender)) {
      return true
    }
    
    // Check contains
    if (this.config.senders.contains.some(s => upperSender.includes(s))) {
      return true
    }
    
    // Check regex patterns
    return this.config.senders.patterns.some(pattern => pattern.test(upperSender))
  }

  /**
   * Parse an SMS message and extract transaction information
   */
  parse(smsBody: string, sender: string, timestamp: number = Date.now()): ParsedTransaction | null {
    // Skip non-transaction messages
    if (!this.isTransactionMessage(smsBody)) {
      console.log('Not a transaction message:', smsBody.substring(0, 100))
      return null
    }

    const amount = this.extractAmount(smsBody)
    if (!amount) {
      console.log('Could not extract amount from:', smsBody.substring(0, 100))
      return null
    }

    const type = this.extractTransactionType(smsBody)
    if (!type) {
      console.log('Could not extract transaction type from:', smsBody.substring(0, 100))
      return null
    }

    const merchant = this.extractMerchant(smsBody, sender)
    const category = CategoryMapper.getCategory(merchant, type)

    return {
      amount,
      type,
      merchant,
      category,
      reference: this.extractReference(smsBody),
      accountLast4: this.extractAccountLast4(smsBody),
      balance: this.extractBalance(smsBody),
      smsBody,
      sender,
      timestamp,
      bankName: this.getBankName()
    }
  }

  /**
   * Check if the message is a transaction message
   */
  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Skip messages with skip patterns
    if (this.config.skipPatterns.some(pattern => lowerMessage.includes(pattern))) {
      return false
    }

    // Must contain transaction keywords
    const transactionKeywords = [
      ...this.config.transactionType.expense,
      ...this.config.transactionType.income
    ]

    return transactionKeywords.some(keyword => lowerMessage.includes(keyword))
  }

  /**
   * Extract transaction amount from the message
   */
  protected extractAmount(message: string): number | null {
    const patterns = [
      /Rs\.?\s*([0-9,]+(?:\.\d{2})?)/i,
      /INR\s*([0-9,]+(?:\.\d{2})?)/i,
      /₹\s*([0-9,]+(?:\.\d{2})?)/,
      /([0-9,]+(?:\.\d{2})?)\s+(?:has\s+been\s+)?(?:debited|credited)/i,
    ]

    for (const pattern of patterns) {
      const match = message.match(pattern)
      if (match) {
        const amountStr = match[1].replace(/,/g, '')
        const amount = parseFloat(amountStr)
        if (!isNaN(amount)) {
          return amount
        }
      }
    }

    return null
  }

  /**
   * Extract transaction type (INCOME/EXPENSE)
   */
  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()

    // Check expense keywords
    if (this.config.transactionType.expense.some(keyword => lowerMessage.includes(keyword))) {
      return TransactionType.EXPENSE
    }

    // Check income keywords
    if (this.config.transactionType.income.some(keyword => lowerMessage.includes(keyword))) {
      return TransactionType.INCOME
    }

    return null
  }

  /**
   * Extract merchant/payee information
   */
  protected extractMerchant(message: string, sender: string): string | null {
    const patterns = [
      /to\s+([^.\n]+?)(?:\s+on|\s+at|\s+Ref|\s+UPI|$)/i,
      /from\s+([^.\n]+?)(?:\s+on|\s+at|\s+Ref|\s+UPI|$)/i,
      /at\s+([^.\n]+?)(?:\s+on|\s+Ref|$)/i,
      /for\s+([^.\n]+?)(?:\s+on|\s+at|\s+Ref|$)/i,
    ]

    for (const pattern of patterns) {
      const match = message.match(pattern)
      if (match) {
        const merchant = this.cleanMerchantName(match[1].trim())
        if (this.isValidMerchantName(merchant)) {
          return merchant
        }
      }
    }

    return null
  }

  /**
   * Extract transaction reference number
   */
  protected extractReference(message: string): string | null {
    const patterns = [
      /(?:Ref|Reference|Txn|Transaction)(?:\s+No)?[:\s]+([A-Z0-9]+)/i,
      /UPI[:\s]+([0-9]+)/i,
      /Reference\s+Number[:\s]+([A-Z0-9]+)/i,
    ]

    for (const pattern of patterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1].trim()
      }
    }

    return null
  }

  /**
   * Extract last 4 digits of account number
   */
  protected extractAccountLast4(message: string): string | null {
    const patterns = [
      /(?:A\/c|Account|Acct)(?:\s+No)?\.?\s+(?:XX+)?(\d{4})/i,
      /Card\s+(?:XX+)?(\d{4})/i,
      /(?:A\/c|Account).*?(\d{4})(?:\s|$)/i,
    ]

    for (const pattern of patterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1]
      }
    }

    return null
  }

  /**
   * Extract balance after transaction
   */
  protected extractBalance(message: string): number | null {
    const patterns = [
      /(?:Bal|Balance|Avl Bal|Available Balance)[:\s]+(?:Rs\.?\s*)?([0-9,]+(?:\.\d{2})?)/i,
      /(?:Updated Balance|Remaining Balance)[:\s]+(?:Rs\.?\s*)?([0-9,]+(?:\.\d{2})?)/i,
    ]

    for (const pattern of patterns) {
      const match = message.match(pattern)
      if (match) {
        const balanceStr = match[1].replace(/,/g, '')
        const balance = parseFloat(balanceStr)
        if (!isNaN(balance)) {
          return balance
        }
      }
    }

    return null
  }

  /**
   * Clean merchant name by removing common suffixes and noise
   */
  protected cleanMerchantName(merchant: string): string {
    return merchant
      .replace(/\(.*?\)$/, '') // Remove trailing parentheses
      .replace(/\s+Ref\s+\d+$/i, '') // Remove ref number suffix
      .replace(/\s+\d{2}\/\d{2}\/\d{2,4}$/i, '') // Remove date suffix
      .replace(/\s+UPI$/i, '') // Remove UPI suffix
      .replace(/\s+\d{2}:\d{2}:\d{2}$/i, '') // Remove time suffix
      .replace(/[-\s]+$/, '') // Remove trailing dash/spaces
      .replace(/\s+PVT\.?\s+LTD\.?$/i, '') // Remove Pvt Ltd
      .replace(/\s+LTD\.?$/i, '') // Remove Ltd
      .trim()
  }

  /**
   * Validate if the extracted merchant name is valid
   */
  protected isValidMerchantName(name: string): boolean {
    const commonWords = new Set(['USING', 'VIA', 'THROUGH', 'BY', 'WITH', 'FOR', 'TO', 'FROM', 'AT', 'THE'])

    return (
      name.length >= 2 &&
      /[a-zA-Z]/.test(name) && // Has at least one letter
      !commonWords.has(name.toUpperCase()) &&
      !/^\d+$/.test(name) && // Not all digits
      !name.includes('@') // Not a UPI ID
    )
  }
}