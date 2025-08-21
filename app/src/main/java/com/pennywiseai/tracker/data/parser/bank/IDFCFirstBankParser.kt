package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal

/**
 * Parser for IDFC First Bank SMS messages
 * 
 * Common senders: XX-IDFCBK-S, XX-IDFCBK-T, XX-IDFCB-S, XX-IDFCB-T, IDFCBK
 * Examples: BM-IDFCBK-S, AX-IDFCBK-T, AD-IDFCB-S
 * 
 * SMS Format:
 * Your A/C XXXXXXXXXXX is debited by INR 68.00 on 06/08/25 17:36. New Bal :INR XXXXX.00
 * Your A/C XXXXXXXXXXX is credited by INR 500.00 on 06/08/25 17:36. New Bal :INR XXXXX.00
 */
class IDFCFirstBankParser : BankParser() {
    
    override fun getBankName() = "IDFC First Bank"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.matches(Regex("^[A-Z]{2}-IDFCBK-S$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-IDFCBK-T$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-IDFCFB-S$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-IDFCFB-T$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-IDFCBK$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-IDFCFB$")) ||
               normalizedSender == "IDFCBK"
    }
    
    override fun extractAmount(message: String): BigDecimal? {
        // Handle debit patterns - "debited by INR" format
        val debitPattern = Regex(
            """(?:is\s+)?debited\s+by\s+INR\s*([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        debitPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Handle credit patterns - "credited by INR" format
        val creditPattern = Regex(
            """(?:is\s+)?credited\s+by\s+INR\s*([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        creditPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        return super.extractAmount(message)
    }
    
    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        return when {
            lowerMessage.contains("debited") -> TransactionType.EXPENSE
            lowerMessage.contains("credited") -> TransactionType.INCOME
            lowerMessage.contains("withdrawn") || lowerMessage.contains("withdrawal") -> TransactionType.EXPENSE
            lowerMessage.contains("deposited") || lowerMessage.contains("deposit") -> TransactionType.INCOME
            else -> super.extractTransactionType(message)
        }
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // UPI transaction pattern
        if (message.contains("UPI", ignoreCase = true)) {
            // Try to extract UPI ID
            val upiPattern = Regex(
                """(?:to|from|at)\s+([a-zA-Z0-9._-]+@[a-zA-Z0-9]+)""",
                RegexOption.IGNORE_CASE
            )
            upiPattern.find(message)?.let { match ->
                return "UPI - ${match.groupValues[1]}"
            }
            return "UPI Transaction"
        }
        
        // NEFT/IMPS/RTGS patterns
        when {
            message.contains("NEFT", ignoreCase = true) -> return "NEFT Transfer"
            message.contains("IMPS", ignoreCase = true) -> return "IMPS Transfer"
            message.contains("RTGS", ignoreCase = true) -> return "RTGS Transfer"
        }
        
        // ATM withdrawal
        if (message.contains("ATM", ignoreCase = true)) {
            val atmPattern = Regex(
                """ATM\s+(?:at\s+)?([^.]+?)(?:\.|,|on|New)""",
                RegexOption.IGNORE_CASE
            )
            atmPattern.find(message)?.let { match ->
                val location = cleanMerchantName(match.groupValues[1])
                return "ATM - $location"
            }
            return "ATM Withdrawal"
        }
        
        // For card transactions
        val toPattern = Regex(
            """(?:to|at|for)\s+([A-Z][A-Z0-9\s&.-]+?)(?:\s+on|\s+New|\.|\,|$)""",
            RegexOption.IGNORE_CASE
        )
        toPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1])
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        return super.extractMerchant(message, sender)
    }
    
    override fun extractAccountLast4(message: String): String? {
        // Pattern: A/C XXXXXXXXXXX where last 4 digits are visible
        val acPattern = Regex(
            """A/C\s+[X]*(\d{3,4})""",
            RegexOption.IGNORE_CASE
        )
        acPattern.find(message)?.let { match ->
            val digits = match.groupValues[1]
            return if (digits.length >= 4) digits.takeLast(4) else digits
        }
        
        return super.extractAccountLast4(message)
    }
    
    override fun extractBalance(message: String): BigDecimal? {
        // Pattern: New Bal :INR XXXXX.00
        val balPattern = Regex(
            """New\s+Bal\s*:\s*INR\s*([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        balPattern.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        return super.extractBalance(message)
    }
    
    override fun extractReference(message: String): String? {
        // UPI reference pattern
        val upiRefPattern = Regex(
            """UPI[:/]\s*([0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        upiRefPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Transaction ID pattern
        val txnIdPattern = Regex(
            """(?:txn|transaction)\s*(?:id|ref|no)[:\s]*([A-Z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        txnIdPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        return super.extractReference(message)
    }
}
