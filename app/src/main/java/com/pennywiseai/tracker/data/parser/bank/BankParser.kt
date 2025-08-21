package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.core.CompiledPatterns
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.parser.ParsedTransaction
import java.math.BigDecimal

/**
 * Base class for bank-specific message parsers.
 * Each bank should extend this class and implement its specific parsing logic.
 */
abstract class BankParser {
    
    /**
     * Returns the name of the bank this parser handles.
     */
    abstract fun getBankName(): String
    
    /**
     * Checks if this parser can handle messages from the given sender.
     */
    abstract fun canHandle(sender: String): Boolean
    
    /**
     * Parses an SMS message and extracts transaction information.
     * Returns null if the message cannot be parsed.
     */
    open fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        // Skip non-transaction messages
        if (!isTransactionMessage(smsBody)) {
            android.util.Log.d("BankParser", "Not a transaction message: ${smsBody.take(100)}")
            return null
        }
        
        val amount = extractAmount(smsBody)
        if (amount == null) {
            android.util.Log.d("BankParser", "Could not extract amount from: ${smsBody.take(100)}")
            return null
        }
        
        val type = extractTransactionType(smsBody)
        if (type == null) {
            android.util.Log.d("BankParser", "Could not extract transaction type from: ${smsBody.take(100)}")
            return null
        }
        
        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = extractMerchant(smsBody, sender),
            reference = extractReference(smsBody),
            accountLast4 = extractAccountLast4(smsBody),
            balance = extractBalance(smsBody),
            smsBody = smsBody,
            sender = sender,
            timestamp = timestamp,
            bankName = getBankName()
        )
    }
    
    /**
     * Checks if the message is a transaction message (not OTP, promotional, etc.)
     */
    protected open fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Skip OTP messages
        if (lowerMessage.contains("otp") || 
            lowerMessage.contains("one time password") ||
            lowerMessage.contains("verification code")) {
            return false
        }
        
        // Skip promotional messages
        if (lowerMessage.contains("offer") || 
            lowerMessage.contains("discount") ||
            lowerMessage.contains("cashback offer") ||
            lowerMessage.contains("win ")) {
            return false
        }
        
        // Skip payment request messages (common across banks)
        if (lowerMessage.contains("has requested") || 
            lowerMessage.contains("payment request") ||
            lowerMessage.contains("collect request") ||
            lowerMessage.contains("requesting payment") ||
            lowerMessage.contains("requests rs") ||
            lowerMessage.contains("ignore if already paid")) {
            return false
        }
        
        // Must contain transaction keywords
        val transactionKeywords = listOf(
            "debited", "credited", "withdrawn", "deposited",
            "spent", "received", "transferred", "paid"
        )
        
        return transactionKeywords.any { lowerMessage.contains(it) }
    }
    
    /**
     * Extracts the transaction amount from the message.
     */
    protected open fun extractAmount(message: String): BigDecimal? {
        for (pattern in CompiledPatterns.Amount.ALL_PATTERNS) {
            pattern.find(message)?.let { match ->
                val amountStr = match.groupValues[1].replace(",", "")
                return try {
                    BigDecimal(amountStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        
        return null
    }
    
    /**
     * Extracts the transaction type (INCOME/EXPENSE).
     */
    protected open fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        
        return when {
            lowerMessage.contains("debited") -> TransactionType.EXPENSE
            lowerMessage.contains("withdrawn") -> TransactionType.EXPENSE
            lowerMessage.contains("spent") -> TransactionType.EXPENSE
            lowerMessage.contains("charged") -> TransactionType.EXPENSE
            lowerMessage.contains("paid") -> TransactionType.EXPENSE
            lowerMessage.contains("purchase") -> TransactionType.EXPENSE
            
            lowerMessage.contains("credited") -> TransactionType.INCOME
            lowerMessage.contains("deposited") -> TransactionType.INCOME
            lowerMessage.contains("received") -> TransactionType.INCOME
            lowerMessage.contains("refund") -> TransactionType.INCOME
            lowerMessage.contains("cashback") && !lowerMessage.contains("earn cashback") -> TransactionType.INCOME
            
            else -> null
        }
    }
    
    /**
     * Extracts merchant/payee information.
     */
    protected open fun extractMerchant(message: String, sender: String): String? {
        for (pattern in CompiledPatterns.Merchant.ALL_PATTERNS) {
            pattern.find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }
        
        return null
    }
    
    /**
     * Extracts transaction reference number.
     */
    protected open fun extractReference(message: String): String? {
        for (pattern in CompiledPatterns.Reference.ALL_PATTERNS) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1].trim()
            }
        }
        
        return null
    }
    
    /**
     * Extracts last 4 digits of account number.
     */
    protected open fun extractAccountLast4(message: String): String? {
        for (pattern in CompiledPatterns.Account.ALL_PATTERNS) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1]
            }
        }
        
        return null
    }
    
    /**
     * Extracts balance after transaction.
     */
    protected open fun extractBalance(message: String): BigDecimal? {
        for (pattern in CompiledPatterns.Balance.ALL_PATTERNS) {
            pattern.find(message)?.let { match ->
                val balanceStr = match.groupValues[1].replace(",", "")
                return try {
                    BigDecimal(balanceStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        
        return null
    }
    
    /**
     * Cleans merchant name by removing common suffixes and noise.
     */
    protected open fun cleanMerchantName(merchant: String): String {
        return merchant
            .replace(CompiledPatterns.Cleaning.TRAILING_PARENTHESES, "")
            .replace(CompiledPatterns.Cleaning.REF_NUMBER_SUFFIX, "")
            .replace(CompiledPatterns.Cleaning.DATE_SUFFIX, "")
            .replace(CompiledPatterns.Cleaning.UPI_SUFFIX, "")
            .replace(CompiledPatterns.Cleaning.TIME_SUFFIX, "")
            .replace(CompiledPatterns.Cleaning.TRAILING_DASH, "")
            .replace(CompiledPatterns.Cleaning.PVT_LTD, "")
            .replace(CompiledPatterns.Cleaning.LTD, "")
            .trim()
    }
    
    /**
     * Validates if the extracted merchant name is valid.
     */
    protected open fun isValidMerchantName(name: String): Boolean {
        val commonWords = setOf("USING", "VIA", "THROUGH", "BY", "WITH", "FOR", "TO", "FROM", "AT", "THE")
        
        return name.length >= Constants.Parsing.MIN_MERCHANT_NAME_LENGTH && 
               name.any { it.isLetter() } && 
               name.uppercase() !in commonWords &&
               !name.all { it.isDigit() } &&
               !name.contains("@") // Not a UPI ID
    }
}
