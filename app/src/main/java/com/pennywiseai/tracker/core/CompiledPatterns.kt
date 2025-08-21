package com.pennywiseai.tracker.core

/**
 * Pre-compiled regex patterns for better performance.
 * Regex compilation is expensive, so we compile once and reuse.
 */
object CompiledPatterns {
    
    /**
     * Amount extraction patterns
     */
    object Amount {
        val RS_PATTERN = Regex("""Rs\.?\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        val INR_PATTERN = Regex("""INR\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        val RUPEE_SYMBOL_PATTERN = Regex("""₹\s*([0-9,]+(?:\.\d{2})?)""")
        
        val ALL_PATTERNS = listOf(RS_PATTERN, INR_PATTERN, RUPEE_SYMBOL_PATTERN)
    }
    
    /**
     * Reference number patterns
     */
    object Reference {
        val GENERIC_REF = Regex("""(?:Ref|Reference|Txn|Transaction)(?:\s+No)?[:\s]+([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
        val UPI_REF = Regex("""UPI[:\s]+([0-9]+)""", RegexOption.IGNORE_CASE)
        val REF_NUMBER = Regex("""Reference\s+Number[:\s]+([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
        
        val ALL_PATTERNS = listOf(GENERIC_REF, UPI_REF, REF_NUMBER)
    }
    
    /**
     * Account number patterns (last 4 digits)
     */
    object Account {
        val AC_WITH_MASK = Regex("""(?:A/c|Account|Acct)(?:\s+No)?\\.?\s+(?:XX+)?(\\d{4})""", RegexOption.IGNORE_CASE)
        val CARD_WITH_MASK = Regex("""Card\s+(?:XX+)?(\\d{4})""", RegexOption.IGNORE_CASE)
        val GENERIC_ACCOUNT = Regex("""(?:A/c|Account).*?(\\d{4})(?:\s|$)""", RegexOption.IGNORE_CASE)
        
        val ALL_PATTERNS = listOf(AC_WITH_MASK, CARD_WITH_MASK, GENERIC_ACCOUNT)
    }
    
    /**
     * Balance patterns
     */
    object Balance {
        val AVL_BAL = Regex("""(?:Bal|Balance|Avl Bal|Available Balance)[:\s]+(?:Rs\.?\s*)?([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        val UPDATED_BAL = Regex("""(?:Updated Balance|Remaining Balance)[:\s]+(?:Rs\.?\s*)?([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        
        val ALL_PATTERNS = listOf(AVL_BAL, UPDATED_BAL)
    }
    
    /**
     * Merchant extraction patterns
     */
    object Merchant {
        val TO_PATTERN = Regex("""to\s+([^.\n]+?)(?:\s+on|\s+at|\s+Ref|\s+UPI)""", RegexOption.IGNORE_CASE)
        val FROM_PATTERN = Regex("""from\s+([^.\n]+?)(?:\s+on|\s+at|\s+Ref|\s+UPI)""", RegexOption.IGNORE_CASE)
        val AT_PATTERN = Regex("""at\s+([^.\n]+?)(?:\s+on|\s+Ref)""", RegexOption.IGNORE_CASE)
        val FOR_PATTERN = Regex("""for\s+([^.\n]+?)(?:\s+on|\s+at|\s+Ref)""", RegexOption.IGNORE_CASE)
        
        val ALL_PATTERNS = listOf(TO_PATTERN, FROM_PATTERN, AT_PATTERN, FOR_PATTERN)
    }
    
    /**
     * Bank-specific patterns
     */
    object HDFC {
        // DLT patterns for sender validation
        val DLT_PATTERNS = listOf(
            Regex("^[A-Z]{2}-HDFCBK.*$"),
            Regex("^[A-Z]{2}-HDFC.*$"),
            Regex("^HDFC-[A-Z]+$"),
            Regex("^[A-Z]{2}-HDFCB.*$")
        )
        
        // Transaction patterns
        val SALARY_PATTERN = Regex("""for\s+[^-]+-[^-]+-[^-]+\s+[A-Z]+\s+SALARY-([^.\n]+)""", RegexOption.IGNORE_CASE)
        val SIMPLE_SALARY_PATTERN = Regex("""SALARY[- ]([^.\n]+?)(?:\s+Info|$)""", RegexOption.IGNORE_CASE)
        val INFO_PATTERN = Regex("""Info:\s*(?:UPI/)?([^/.\n]+?)(?:/|$)""", RegexOption.IGNORE_CASE)
        val VPA_WITH_NAME = Regex("""VPA\s+[^@\s]+@[^\s]+\s*\(([^)]+)\)""", RegexOption.IGNORE_CASE)
        val VPA_PATTERN = Regex("""VPA\s+([^@\s]+)@""", RegexOption.IGNORE_CASE)
        val SPENT_PATTERN = Regex("""at\s+([^.\n]+?)\s+on\s+\d{2}""", RegexOption.IGNORE_CASE)
        val DEBIT_FOR_PATTERN = Regex("""debited\s+for\s+([^.\n]+?)\s+on\s+\d{2}""", RegexOption.IGNORE_CASE)
        val MANDATE_PATTERN = Regex("""To\s+([^\n]+?)\s*(?:\n|\d{2}/\d{2})""", RegexOption.IGNORE_CASE)
        
        // Reference patterns
        val REF_SIMPLE = Regex("""Ref\s+(\d{9,12})""", RegexOption.IGNORE_CASE)
        val UPI_REF_NO = Regex("""UPI\s+Ref\s+No\s+(\d{12})""", RegexOption.IGNORE_CASE)
        val REF_NO = Regex("""Ref\s+No\.?\s+([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
        val REF_END = Regex("""(?:Ref|Reference)[:.\s]+([A-Z0-9]{6,})(?:\s*$|\s*Not\s+You)""", RegexOption.IGNORE_CASE)
        
        // Account patterns
        val ACCOUNT_DEPOSITED = Regex("""deposited\s+in\s+(?:HDFC\s+Bank\s+)?A/c\s+(?:XX+)?(\d{4})""", RegexOption.IGNORE_CASE)
        val ACCOUNT_FROM = Regex("""from\s+(?:HDFC\s+Bank\s+)?A/c\s+(?:XX+)?(\d{4})""", RegexOption.IGNORE_CASE)
        val ACCOUNT_SIMPLE = Regex("""HDFC\s+Bank\s+A/c\s+(\d{4})""", RegexOption.IGNORE_CASE)
        val ACCOUNT_GENERIC = Regex("""A/c\s+(?:XX+)?(\d{4})""", RegexOption.IGNORE_CASE)
        
        // E-Mandate patterns (multi-line format)
        val AMOUNT_WILL_DEDUCT = Regex("""Rs\.?\s*([0-9,]+(?:\.\d{2})?)\s+will\s+be\s+deducted""", RegexOption.IGNORE_CASE)
        val DEDUCTION_DATE = Regex("""deducted\s+on\s+(\d{2}/\d{2}/\d{2}),?\s*\d{2}:\d{2}:\d{2}""", RegexOption.IGNORE_CASE)
        val MANDATE_MERCHANT = Regex("""For\s+([^\n]+?)\s+mandate""", RegexOption.IGNORE_CASE)
        val UMN_PATTERN = Regex("""UMN\s+([a-zA-Z0-9@]+)""", RegexOption.IGNORE_CASE)
    }
    
    /**
     * Cleaning patterns
     */
    object Cleaning {
        val TRAILING_PARENTHESES = Regex("""\s*\(.*?\)\s*$""")
        val REF_NUMBER_SUFFIX = Regex("""\s+Ref\s+No.*""", RegexOption.IGNORE_CASE)
        val DATE_SUFFIX = Regex("""\s+on\s+\d{2}.*""")
        val UPI_SUFFIX = Regex("""\s+UPI.*""", RegexOption.IGNORE_CASE)
        val TIME_SUFFIX = Regex("""\s+at\s+\d{2}:\d{2}.*""")
        val TRAILING_DASH = Regex("""\s*-\s*$""")
        
        // Company suffixes
        val PVT_LTD = Regex("""(\s+PVT\.?\s*LTD\.?|\s+PRIVATE\s+LIMITED)$""", RegexOption.IGNORE_CASE)
        val LTD = Regex("""(\s+LTD\.?|\s+LIMITED)$""", RegexOption.IGNORE_CASE)
    }
}