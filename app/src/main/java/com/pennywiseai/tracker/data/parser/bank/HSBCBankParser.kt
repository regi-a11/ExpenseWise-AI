package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.parser.ParsedTransaction
import java.math.BigDecimal

/**
 * Parser for HSBC Bank SMS messages
 */
class HSBCBankParser : BankParser() {
    
    override fun getBankName() = "HSBC Bank"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("HSBC") ||
               normalizedSender.contains("HSBCIN") ||
               // DLT patterns
               normalizedSender.matches(Regex("^[A-Z]{2}-HSBCIN-[A-Z]$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-HSBC-[A-Z]$"))
    }
    
    override fun parse(
        smsBody: String,
        sender: String,
        timestamp: Long
    ): ParsedTransaction? {
        if (!canHandle(sender)) return null
        if (!isTransactionMessage(smsBody)) return null
        
        val amount = extractAmount(smsBody) ?: return null
        val transactionType = extractTransactionType(smsBody) ?: return null
        val merchant = extractMerchant(smsBody, sender) ?: "Unknown"
        
        return ParsedTransaction(
            amount = amount,
            type = transactionType,
            merchant = merchant,
            accountLast4 = extractAccountLast4(smsBody),
            balance = extractBalance(smsBody),
            reference = extractReference(smsBody),
            smsBody = smsBody,
            sender = sender,
            timestamp = timestamp,
            bankName = getBankName()
        )
    }
    
    override fun extractAmount(message: String): BigDecimal? {
        // Pattern: INR 49.00 is paid from
        // Pattern: INR 1000.50 is credited to
        val pattern = Regex(
            """INR\s+([\d,]+(?:\.\d{2})?)\s+is\s+(?:paid|credited|debited)""",
            RegexOption.IGNORE_CASE
        )
        
        pattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        return super.extractAmount(message)
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // Pattern 1: "to [Merchant] on" for payments
        val paymentPattern = Regex(
            """to\s+([^.]+?)\s+on\s+\d""",
            RegexOption.IGNORE_CASE
        )
        paymentPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        // Pattern 2: "from [Merchant]" for credits
        val creditPattern = Regex(
            """from\s+([^.]+?)(?:\s+on\s+|\s+with\s+|$)""",
            RegexOption.IGNORE_CASE
        )
        creditPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        return super.extractMerchant(message, sender)
    }
    
    override fun extractAccountLast4(message: String): String? {
        // Pattern: account XXXXXX1234
        val pattern = Regex(
            """account\s+[X*]+(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        pattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        return super.extractAccountLast4(message)
    }
    
    override fun extractReference(message: String): String? {
        // Pattern: with ref 222222222222
        val pattern = Regex(
            """with\s+ref\s+(\w+)""",
            RegexOption.IGNORE_CASE
        )
        pattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        return super.extractReference(message)
    }
    
    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        
        return when {
            lowerMessage.contains("is paid from") -> TransactionType.EXPENSE
            lowerMessage.contains("is debited") -> TransactionType.EXPENSE
            lowerMessage.contains("is credited to") -> TransactionType.INCOME
            lowerMessage.contains("deposited") -> TransactionType.INCOME
            else -> super.extractTransactionType(message)
        }
    }
    
    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Check for HSBC-specific transaction keywords
        if (lowerMessage.contains("is paid from") ||
            lowerMessage.contains("is credited to") ||
            lowerMessage.contains("is debited") ||
            (lowerMessage.contains("inr") && lowerMessage.contains("account"))) {
            return true
        }
        
        return super.isTransactionMessage(message)
    }
}