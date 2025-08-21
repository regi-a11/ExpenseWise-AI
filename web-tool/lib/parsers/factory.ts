import { BankParser } from './base'
import { HDFCBankParser } from './banks/hdfc'
import { SBIBankParser } from './banks/sbi'
import { ICICIBankParser } from './banks/icici'
import { AxisBankParser } from './banks/axis'
import { FederalBankParser } from './banks/federal'
import { IndianBankParser } from './banks/indian'
import { BankOfBarodaParser } from './banks/bob'
import { CanaraParser } from './banks/canara'
import { PNBParser } from './banks/pnb'
import { IDBIBankParser } from './banks/idbi'
import { KarnatakaBankParser } from './banks/karnataka'
import { JupiterParser } from './banks/jupiter'
import { JioPaymentsBankParser } from './banks/jiopayments'
import { JuspayParser } from './banks/juspay'
import { SliceParser } from './banks/slice'
import { UtkarshBankParser } from './banks/utkarsh'
import { KotakBankParser } from './banks/kotak'
import { IDFCFirstBankParser } from './banks/idfc'
import { UnionBankParser } from './banks/union'
import { HSBCBankParser } from './banks/hsbc'
import { CentralBankOfIndiaParser } from './banks/centralbank'
import { ParseResult, ParsedTransaction } from './types'

export class BankParserFactory {
  private static parsers: BankParser[] = [
    new HDFCBankParser(),
    new SBIBankParser(),
    new ICICIBankParser(),
    new AxisBankParser(),
    new FederalBankParser(),
    new IndianBankParser(),
    new BankOfBarodaParser(),
    new CanaraParser(),
    new PNBParser(),
    new IDBIBankParser(),
    new KarnatakaBankParser(),
    new JupiterParser(),
    new JioPaymentsBankParser(),
    new JuspayParser(),
    new SliceParser(),
    new UtkarshBankParser(),
    new KotakBankParser(),
    new IDFCFirstBankParser(),
    new UnionBankParser(),
    new HSBCBankParser(),
    new CentralBankOfIndiaParser(),
  ]

  /**
   * Get a parser that can handle the given sender
   */
  static getParser(sender: string): BankParser | null {
    for (const parser of this.parsers) {
      if (parser.canHandle(sender)) {
        return parser
      }
    }
    return null
  }

  /**
   * Parse an SMS message
   */
  static parse(smsBody: string, sender: string, timestamp?: number): ParseResult {
    const parser = this.getParser(sender)
    
    if (!parser) {
      return {
        success: false,
        error: `No parser found for sender: ${sender}`
      }
    }

    try {
      const result = parser.parse(smsBody, sender, timestamp)
      
      if (result) {
        return {
          success: true,
          data: result,
          confidence: this.calculateConfidence(result)
        }
      } else {
        return {
          success: false,
          error: 'Failed to parse transaction data from SMS'
        }
      }
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown parsing error'
      }
    }
  }

  /**
   * Get all supported bank codes
   */
  static getSupportedBanks(): Array<{ code: string; name: string }> {
    return this.parsers.map(parser => ({
      code: parser.getBankCode(),
      name: parser.getBankName()
    }))
  }

  /**
   * Check if a sender is supported
   */
  static isSenderSupported(sender: string): boolean {
    return this.getParser(sender) !== null
  }

  /**
   * Calculate confidence score for parsed transaction
   */
  private static calculateConfidence(transaction: ParsedTransaction): number {
    let score = 0
    let maxScore = 0

    // Amount is required, so if we have it, good confidence
    if (transaction.amount !== null) {
      score += 30
    }
    maxScore += 30

    // Transaction type is required
    if (transaction.type !== null) {
      score += 20
    }
    maxScore += 20

    // Merchant is important
    if (transaction.merchant !== null) {
      score += 20
    }
    maxScore += 20

    // Reference adds confidence
    if (transaction.reference !== null) {
      score += 10
    }
    maxScore += 10

    // Account number adds confidence
    if (transaction.accountLast4 !== null) {
      score += 10
    }
    maxScore += 10

    // Balance adds confidence
    if (transaction.balance !== null) {
      score += 10
    }
    maxScore += 10

    return score / maxScore
  }

  /**
   * Get parser statistics
   */
  static getStatistics(): {
    totalParsers: number
    supportedBanks: string[]
  } {
    return {
      totalParsers: this.parsers.length,
      supportedBanks: this.parsers.map(p => p.getBankName())
    }
  }
}