// Parser types and interfaces

export enum TransactionType {
  INCOME = 'INCOME',
  EXPENSE = 'EXPENSE'
}

export interface ParsedTransaction {
  amount: number | null
  type: TransactionType | null
  merchant: string | null
  category: string | null
  reference: string | null
  accountLast4: string | null
  balance: number | null
  smsBody: string
  sender: string
  timestamp: number
  bankName: string
}

export interface ParseResult {
  success: boolean
  data?: ParsedTransaction
  error?: string
  confidence?: number
}

export interface BankPattern {
  amount: RegExp[]
  merchant: RegExp[]
  reference?: RegExp[]
  account?: RegExp[]
  balance?: RegExp[]
}

export interface BankConfig {
  bankName: string
  bankCode: string
  senders: {
    exact: string[]
    contains: string[]
    patterns: RegExp[]
  }
  transactionType: {
    expense: string[]
    income: string[]
  }
  skipPatterns: string[]
}