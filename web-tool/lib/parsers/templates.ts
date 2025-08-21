// Bank message templates and patterns
// This shows what types of messages each bank parser can handle

export interface MessageTemplate {
  name: string
  description: string
  example: string
  fields: string[]
  category?: 'debit' | 'credit' | 'atm' | 'upi' | 'salary' | 'card' | 'mandate' | 'other'
}

export const bankTemplates: Record<string, {
  bankName: string
  supportLevel: 'full' | 'partial' | 'experimental'
  templates: MessageTemplate[]
}> = {
  hdfc: {
    bankName: 'HDFC Bank',
    supportLevel: 'full',
    templates: [
      {
        name: 'Standard Debit',
        category: 'debit',
        description: 'Regular debit transactions',
        example: 'Rs.500 debited from A/c XX1234 for merchant on 15-01-24',
        fields: ['amount', 'account', 'merchant', 'date']
      },
      {
        name: 'Standard Credit',
        category: 'credit',
        description: 'Regular credit transactions',
        example: 'Rs.1000 credited to A/c XX1234 on 15-01-24',
        fields: ['amount', 'account', 'date']
      },
      {
        name: 'UPI Transaction',
        category: 'upi',
        description: 'UPI payments with VPA details',
        example: 'Rs.200 debited from A/c XX1234 VPA merchant@paytm (Merchant Name) Info: UPI/Food',
        fields: ['amount', 'account', 'vpa', 'merchant', 'category']
      },
      {
        name: 'Salary Credit',
        category: 'salary',
        description: 'Monthly salary deposits with company name',
        example: 'Rs.50000 deposited in HDFC Bank A/c XX1234 for EMP-ID-123 JAN SALARY-COMPANY NAME',
        fields: ['amount', 'account', 'company', 'month']
      },
      {
        name: 'Card Transaction',
        category: 'card',
        description: 'Credit/Debit card transactions',
        example: 'Rs.1500 spent on Card XX1234 at Amazon on 15-01-24',
        fields: ['amount', 'card', 'merchant', 'date']
      },
      {
        name: 'E-Mandate',
        category: 'mandate',
        description: 'Recurring payment notifications',
        example: 'E-Mandate! Rs.599 will be deducted on 15/01/24 For Netflix mandate UMN netflix@123',
        fields: ['amount', 'date', 'merchant', 'umn']
      },
      {
        name: 'ATM Withdrawal',
        category: 'atm',
        description: 'ATM cash withdrawals',
        example: 'Rs.5000 withdrawn from A/c XX1234 at ATM Location. Avl Bal Rs.10000',
        fields: ['amount', 'account', 'location', 'balance']
      }
    ]
  },
  sbi: {
    bankName: 'State Bank of India',
    supportLevel: 'full',
    templates: [
      {
        name: 'Standard Debit',
        category: 'debit',
        description: 'Regular debit transactions',
        example: 'Rs 500 debited from A/c XX1234 on 15-01-24',
        fields: ['amount', 'account', 'date']
      },
      {
        name: 'UPI Transaction',
        category: 'upi',
        description: 'UPI payments',
        example: 'A/c debited by 20.0 trf to Mrs Shopkeeper Ref No 123456',
        fields: ['amount', 'merchant', 'reference']
      },
      {
        name: 'ATM Withdrawal',
        category: 'atm',
        description: 'ATM cash withdrawals',
        example: 'ATM withdrawal of Rs 5000 from A/c XX1234 on 15-01-24 Avl Bal Rs 10000',
        fields: ['amount', 'account', 'date', 'balance']
      },
      {
        name: 'YONO Cash',
        category: 'atm',
        description: 'Cardless ATM withdrawal',
        example: 'Yono Cash Rs 3000 w/d@SBI ATM S1NW000093009 on 15-01-24',
        fields: ['amount', 'atm_code', 'date']
      },
      {
        name: 'NEFT/IMPS',
        category: 'credit',
        description: 'NEFT/IMPS transfers',
        example: 'Rs 5000 credited to A/c XX1234 through NEFT from SENDER NAME Ref No 123456',
        fields: ['amount', 'account', 'sender', 'reference']
      }
    ]
  }
}

export function getBankTemplates(bankCode: string) {
  return bankTemplates[bankCode.toLowerCase()] || null
}

export function getAllSupportedTemplates() {
  return Object.entries(bankTemplates).map(([code, data]) => ({
    code,
    ...data
  }))
}