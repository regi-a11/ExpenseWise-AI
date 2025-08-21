export interface CategoryInfo {
  displayName: string
  icon: string // emoji
  color: string // hex color
  keywords: string[]
}

// Categories matching exactly with Kotlin CategoryMapping
export const categories: Record<string, CategoryInfo> = {
  'Food & Dining': {
    displayName: 'Food & Dining',
    icon: '🍔',
    color: '#FC8019', // Swiggy orange
    keywords: ['swiggy', 'zomato', 'dominos', 'pizza', 'burger', 'kfc', 'mcdonalds', 'restaurant', 'cafe', 'food', 'starbucks', 'haldiram', 'barbeque']
  },
  'Groceries': {
    displayName: 'Groceries',
    icon: '🛒',
    color: '#5AC85A', // BigBasket green
    keywords: ['bigbasket', 'blinkit', 'zepto', 'grofers', 'jiomart', 'dmart', 'reliance fresh', 'more', 'grocery', 'dunzo']
  },
  'Transportation': {
    displayName: 'Transportation',
    icon: '🚗',
    color: '#000000', // Uber black
    keywords: ['uber', 'ola', 'rapido', 'metro', 'irctc', 'redbus', 'makemytrip', 'goibibo', 'petrol', 'fuel', 'parking', 'toll', 'fastag', 'indigo', 'air india', 'spicejet', 'vistara', 'cleartrip']
  },
  'Shopping': {
    displayName: 'Shopping',
    icon: '🛍️',
    color: '#FF9900', // Amazon orange
    keywords: ['amazon', 'flipkart', 'myntra', 'ajio', 'nykaa', 'meesho', 'snapdeal', 'shopclues', 'firstcry', 'pepperfry', 'urban ladder', 'store']
  },
  'Bills & Utilities': {
    displayName: 'Bills & Utilities',
    icon: '📄',
    color: '#4CAF50', // Utility green
    keywords: ['electricity', 'water', 'gas', 'broadband', 'wifi', 'internet', 'tata sky', 'dish', 'd2h', 'bill', 'tata power', 'adani', 'bses', 'act fibernet']
  },
  'Entertainment': {
    displayName: 'Entertainment',
    icon: '🎬',
    color: '#E50914', // Netflix red
    keywords: ['netflix', 'spotify', 'prime', 'hotstar', 'sony liv', 'zee5', 'voot', 'youtube', 'cinema', 'pvr', 'inox', 'bookmyshow', 'gaana', 'jiosaavn', 'apple music', 'wynk']
  },
  'Healthcare': {
    displayName: 'Healthcare',
    icon: '💊',
    color: '#10847E', // PharmEasy teal
    keywords: ['1mg', 'pharmeasy', 'netmeds', 'apollo', 'pharmacy', 'medical', 'hospital', 'clinic', 'doctor', 'practo', 'healthkart', 'truemeds']
  },
  'Investments': {
    displayName: 'Investments',
    icon: '📈',
    color: '#00D09C', // Groww green
    keywords: ['groww', 'zerodha', 'upstox', 'kuvera', 'paytm money', 'coin', 'smallcase', 'mutual fund', 'sip', 'angel', '5paisa', 'etmoney']
  },
  'Banking': {
    displayName: 'Banking',
    icon: '🏦',
    color: '#004C8F', // HDFC blue
    keywords: ['hdfc', 'icici', 'axis', 'sbi', 'kotak', 'bank', 'loan', 'emi', 'credit card', 'yes bank', 'idfc', 'indusind', 'pnb', 'canara', 'union bank', 'rbl']
  },
  'Personal Care': {
    displayName: 'Personal Care',
    icon: '💆',
    color: '#6A4C93', // Urban Company purple
    keywords: ['urban company', 'salon', 'spa', 'barber', 'beauty', 'grooming', 'housejoy']
  },
  'Education': {
    displayName: 'Education',
    icon: '🎓',
    color: '#673AB7', // Byju's purple
    keywords: ['byju', 'unacademy', 'vedantu', 'coursera', 'udemy', 'upgrade', 'school', 'college', 'university', 'toppr', 'udacity', 'simplilearn', 'whitehat', 'great learning']
  },
  'Mobile': {
    displayName: 'Mobile & Recharge',
    icon: '📱',
    color: '#2A3890', // Jio blue
    keywords: ['airtel', 'jio', 'vodafone', 'idea', 'bsnl', 'recharge', 'prepaid', 'postpaid', 'mobile']
  },
  'Fitness': {
    displayName: 'Fitness',
    icon: '💪',
    color: '#FF3278', // Cult.fit pink
    keywords: ['cult', 'gym', 'fitness', 'yoga', 'healthifyme', 'fitternity', "gold's gym", 'anytime fitness']
  },
  'Insurance': {
    displayName: 'Insurance',
    icon: '🛡️',
    color: '#0066CC', // LIC blue
    keywords: ['insurance', 'lic', 'policy', 'hdfc life', 'icici pru', 'sbi life', 'max life', 'bajaj allianz', 'policybazaar', 'acko', 'digit']
  },
  // Income-specific categories
  'Salary': {
    displayName: 'Salary',
    icon: '💰',
    color: '#4CAF50', // Income green
    keywords: ['salary', 'payroll', 'wages']
  },
  'Refunds': {
    displayName: 'Refunds',
    icon: '↩️',
    color: '#4CAF50',
    keywords: ['refund']
  },
  'Cashback': {
    displayName: 'Cashback',
    icon: '💸',
    color: '#4CAF50',
    keywords: ['cashback']
  },
  'Interest': {
    displayName: 'Interest',
    icon: '📊',
    color: '#4CAF50',
    keywords: ['interest']
  },
  'Dividends': {
    displayName: 'Dividends',
    icon: '📈',
    color: '#4CAF50',
    keywords: ['dividend']
  },
  'Income': {
    displayName: 'Other Income',
    icon: '💵',
    color: '#4CAF50', // Income green
    keywords: []
  },
  'Others': {
    displayName: 'Others',
    icon: '📦',
    color: '#757575', // Grey
    keywords: []
  }
}

export class CategoryMapper {
  /**
   * Get category for a merchant name (matching Kotlin logic exactly)
   */
  static getCategory(merchantName: string | null, transactionType?: 'INCOME' | 'EXPENSE'): string {
    if (!merchantName) {
      return 'Others'
    }

    const merchantLower = merchantName.toLowerCase()

    // Special handling for income transactions (matching Kotlin ParsedTransaction.determineCategory)
    if (transactionType === 'INCOME') {
      if (merchantLower.includes('salary')) return 'Salary'
      if (merchantLower.includes('refund')) return 'Refunds'
      if (merchantLower.includes('cashback')) return 'Cashback'
      if (merchantLower.includes('interest')) return 'Interest'
      if (merchantLower.includes('dividend')) return 'Dividends'
      return 'Income'
    }

    // Check in order matching Kotlin CategoryMapping.getCategory
    
    // Food & Dining
    if (this.isFoodMerchant(merchantLower)) return 'Food & Dining'
    
    // Groceries
    if (this.isGroceryMerchant(merchantLower)) return 'Groceries'
    
    // Transportation
    if (this.isTransportMerchant(merchantLower)) return 'Transportation'
    
    // Shopping
    if (this.isShoppingMerchant(merchantLower)) return 'Shopping'
    
    // Bills & Utilities
    if (this.isUtilityMerchant(merchantLower)) return 'Bills & Utilities'
    
    // Entertainment
    if (this.isEntertainmentMerchant(merchantLower)) return 'Entertainment'
    
    // Healthcare
    if (this.isHealthcareMerchant(merchantLower)) return 'Healthcare'
    
    // Investments
    if (this.isInvestmentMerchant(merchantLower)) return 'Investments'
    
    // Banking
    if (this.isBankingMerchant(merchantLower)) return 'Banking'
    
    // Personal Care
    if (this.isPersonalCareMerchant(merchantLower)) return 'Personal Care'
    
    // Education
    if (this.isEducationMerchant(merchantLower)) return 'Education'
    
    // Mobile
    if (this.isMobileMerchant(merchantLower)) return 'Mobile'
    
    // Fitness
    if (this.isFitnessMerchant(merchantLower)) return 'Fitness'
    
    // Insurance
    if (this.isInsuranceMerchant(merchantLower)) return 'Insurance'

    return 'Others'
  }

  // Merchant detection functions matching Kotlin exactly
  private static isFoodMerchant(merchant: string): boolean {
    return merchant.includes('swiggy') || merchant.includes('zomato') || 
           merchant.includes('dominos') || merchant.includes('pizza') ||
           merchant.includes('burger') || merchant.includes('kfc') ||
           merchant.includes('mcdonalds') || merchant.includes('restaurant') ||
           merchant.includes('cafe') || merchant.includes('food') ||
           merchant.includes('starbucks') || merchant.includes('haldiram') ||
           merchant.includes('barbeque')
  }

  private static isGroceryMerchant(merchant: string): boolean {
    return merchant.includes('bigbasket') || merchant.includes('blinkit') ||
           merchant.includes('zepto') || merchant.includes('grofers') ||
           merchant.includes('jiomart') || merchant.includes('dmart') ||
           merchant.includes('reliance fresh') || merchant.includes('more') ||
           merchant.includes('grocery') || merchant.includes('dunzo')
  }

  private static isTransportMerchant(merchant: string): boolean {
    return merchant.includes('uber') || merchant.includes('ola') ||
           merchant.includes('rapido') || merchant.includes('metro') ||
           merchant.includes('irctc') || merchant.includes('redbus') ||
           merchant.includes('makemytrip') || merchant.includes('goibibo') ||
           merchant.includes('petrol') || merchant.includes('fuel') ||
           merchant.includes('parking') || merchant.includes('toll') ||
           merchant.includes('fastag') || merchant.includes('indigo') ||
           merchant.includes('air india') || merchant.includes('spicejet') ||
           merchant.includes('vistara') || merchant.includes('cleartrip')
  }

  private static isShoppingMerchant(merchant: string): boolean {
    return merchant.includes('amazon') || merchant.includes('flipkart') ||
           merchant.includes('myntra') || merchant.includes('ajio') ||
           merchant.includes('nykaa') || merchant.includes('meesho') ||
           merchant.includes('snapdeal') || merchant.includes('shopclues') ||
           merchant.includes('firstcry') || merchant.includes('pepperfry') ||
           merchant.includes('urban ladder') || merchant.includes('store') ||
           (merchant.includes('mart') && !merchant.includes('jiomart') && !merchant.includes('dmart'))
  }

  private static isUtilityMerchant(merchant: string): boolean {
    return merchant.includes('electricity') || merchant.includes('water') ||
           merchant.includes('gas') || merchant.includes('broadband') ||
           merchant.includes('wifi') || merchant.includes('internet') ||
           merchant.includes('tata sky') || merchant.includes('dish') ||
           merchant.includes('d2h') || merchant.includes('bill') ||
           merchant.includes('tata power') || merchant.includes('adani') ||
           merchant.includes('bses') || merchant.includes('act fibernet')
  }

  private static isEntertainmentMerchant(merchant: string): boolean {
    return merchant.includes('netflix') || merchant.includes('spotify') ||
           merchant.includes('prime') || merchant.includes('hotstar') ||
           merchant.includes('sony liv') || merchant.includes('zee5') ||
           merchant.includes('voot') || merchant.includes('youtube') ||
           merchant.includes('cinema') || merchant.includes('pvr') ||
           merchant.includes('inox') || merchant.includes('bookmyshow') ||
           merchant.includes('gaana') || merchant.includes('jiosaavn') ||
           merchant.includes('apple music') || merchant.includes('wynk')
  }

  private static isHealthcareMerchant(merchant: string): boolean {
    return merchant.includes('1mg') || merchant.includes('pharmeasy') ||
           merchant.includes('netmeds') || merchant.includes('apollo') ||
           merchant.includes('pharmacy') || merchant.includes('medical') ||
           merchant.includes('hospital') || merchant.includes('clinic') ||
           merchant.includes('doctor') || merchant.includes('practo') ||
           merchant.includes('healthkart') || merchant.includes('truemeds')
  }

  private static isInvestmentMerchant(merchant: string): boolean {
    return merchant.includes('groww') || merchant.includes('zerodha') ||
           merchant.includes('upstox') || merchant.includes('kuvera') ||
           merchant.includes('paytm money') || merchant.includes('coin') ||
           merchant.includes('smallcase') || merchant.includes('mutual fund') ||
           merchant.includes('sip') || merchant.includes('angel') ||
           merchant.includes('5paisa') || merchant.includes('etmoney')
  }

  private static isBankingMerchant(merchant: string): boolean {
    return merchant.includes('hdfc') || merchant.includes('icici') ||
           merchant.includes('axis') || merchant.includes('sbi') ||
           merchant.includes('kotak') || merchant.includes('bank') ||
           merchant.includes('loan') || merchant.includes('emi') ||
           merchant.includes('credit card') || merchant.includes('yes bank') ||
           merchant.includes('idfc') || merchant.includes('indusind') ||
           merchant.includes('pnb') || merchant.includes('canara') ||
           merchant.includes('union bank') || merchant.includes('rbl')
  }

  private static isPersonalCareMerchant(merchant: string): boolean {
    return merchant.includes('urban company') || merchant.includes('salon') ||
           merchant.includes('spa') || merchant.includes('barber') ||
           merchant.includes('beauty') || merchant.includes('grooming') ||
           merchant.includes('housejoy')
  }

  private static isEducationMerchant(merchant: string): boolean {
    return merchant.includes('byju') || merchant.includes('unacademy') ||
           merchant.includes('vedantu') || merchant.includes('coursera') ||
           merchant.includes('udemy') || merchant.includes('upgrade') ||
           merchant.includes('school') || merchant.includes('college') ||
           merchant.includes('university') || merchant.includes('toppr') ||
           merchant.includes('udacity') || merchant.includes('simplilearn') ||
           merchant.includes('whitehat') || merchant.includes('great learning')
  }

  private static isMobileMerchant(merchant: string): boolean {
    return merchant.includes('airtel') || merchant.includes('jio') ||
           merchant.includes('vodafone') || merchant.includes('idea') ||
           merchant.includes('bsnl') || merchant.includes('recharge') ||
           merchant.includes('prepaid') || merchant.includes('postpaid') ||
           merchant.includes('mobile')
  }

  private static isFitnessMerchant(merchant: string): boolean {
    return merchant.includes('cult') || merchant.includes('gym') ||
           merchant.includes('fitness') || merchant.includes('yoga') ||
           merchant.includes('healthifyme') || merchant.includes('fitternity') ||
           merchant.includes("gold's gym") || merchant.includes('anytime fitness')
  }

  private static isInsuranceMerchant(merchant: string): boolean {
    return merchant.includes('insurance') || merchant.includes('lic') ||
           merchant.includes('policy') || merchant.includes('hdfc life') ||
           merchant.includes('icici pru') || merchant.includes('sbi life') ||
           merchant.includes('max life') || merchant.includes('bajaj allianz') ||
           merchant.includes('policybazaar') || merchant.includes('acko') ||
           merchant.includes('digit')
  }

  /**
   * Get category info
   */
  static getCategoryInfo(merchantName: string | null, transactionType?: 'INCOME' | 'EXPENSE'): CategoryInfo {
    const categoryCode = this.getCategory(merchantName, transactionType)
    return categories[categoryCode] || categories['Others']
  }

  /**
   * Get all categories as array
   */
  static getAllCategories(): Array<{ code: string } & CategoryInfo> {
    return Object.entries(categories).map(([code, info]) => ({
      code,
      ...info
    }))
  }

  /**
   * Get categories for display (exclude income categories for expense context)
   */
  static getExpenseCategories(): Array<{ code: string } & CategoryInfo> {
    return Object.entries(categories)
      .filter(([code]) => !['Salary', 'Refunds', 'Cashback', 'Interest', 'Dividends', 'Income'].includes(code))
      .map(([code, info]) => ({
        code,
        ...info
      }))
  }

  /**
   * Check if merchant is likely income based on keywords
   */
  static isIncomeMerchant(merchantName: string | null): boolean {
    if (!merchantName) return false
    
    const merchantLower = merchantName.toLowerCase()
    const incomeKeywords = [
      'salary', 'payroll', 'wages', 'refund', 'cashback', 
      'interest', 'dividend', 'return', 'reimbursement'
    ]
    
    return incomeKeywords.some(keyword => merchantLower.includes(keyword))
  }
}

// Export for convenience
export { CategoryMapper as Categorization }