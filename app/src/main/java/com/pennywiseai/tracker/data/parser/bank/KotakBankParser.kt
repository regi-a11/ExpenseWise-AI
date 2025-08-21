package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.parser.ParsedTransaction
import java.math.BigDecimal

/**
 * Kotak Bank specific parser.
 * Handles Kotak Bank's unique message formats including:
 * - UPI transactions with recipient details
 * - Standard debit/credit messages
 * - Card transactions
 */
class KotakBankParser : BankParser() {
    
    override fun getBankName() = "Kotak Bank"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        
        // DLT patterns for Kotak Bank
        return normalizedSender.matches(Regex("^[A-Z]{2}-KOTAKB-[ST]$"))
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // Pattern 1: "Sent Rs.X from Kotak Bank AC XXXX to merchant@bank on"
        // Extract merchant from UPI ID like "upiswiggy@icici" or "amazonpayrecharges@apl"
        val toPattern = Regex("to\\s+([^\\s]+@[^\\s]+)\\s+on", RegexOption.IGNORE_CASE)
        toPattern.find(message)?.let { match ->
            val upiId = match.groupValues[1].trim()
            
            // Extract merchant name from UPI ID
            val merchantName = when {
                // Handle "upiXXX@bank" format - remove "upi" prefix
                upiId.startsWith("upi", ignoreCase = true) -> {
                    val name = upiId.substring(3).substringBefore("@")
                    if (name.isNotEmpty()) cleanMerchantName(name) else null
                }
                // Handle other UPI IDs - extract username part
                else -> {
                    val name = upiId.substringBefore("@")
                    if (name.isNotEmpty() && !name.all { it.isDigit() }) {
                        cleanMerchantName(name)
                    } else null
                }
            }
            
            if (merchantName != null && isValidMerchantName(merchantName)) {
                return merchantName
            }
        }
        
        // Fall back to generic extraction
        return super.extractMerchant(message, sender)
    }
    
    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        
        return when {
            // Kotak specific: "Sent Rs.X from Kotak Bank"
            lowerMessage.contains("sent") && lowerMessage.contains("from kotak") -> TransactionType.EXPENSE
            
            // Standard expense keywords
            lowerMessage.contains("debited") -> TransactionType.EXPENSE
            lowerMessage.contains("withdrawn") -> TransactionType.EXPENSE
            lowerMessage.contains("spent") -> TransactionType.EXPENSE
            lowerMessage.contains("charged") -> TransactionType.EXPENSE
            lowerMessage.contains("paid") -> TransactionType.EXPENSE
            lowerMessage.contains("purchase") -> TransactionType.EXPENSE
            
            // Income keywords
            lowerMessage.contains("credited") -> TransactionType.INCOME
            lowerMessage.contains("deposited") -> TransactionType.INCOME
            lowerMessage.contains("received") -> TransactionType.INCOME
            lowerMessage.contains("refund") -> TransactionType.INCOME
            lowerMessage.contains("cashback") && !lowerMessage.contains("earn cashback") -> TransactionType.INCOME
            
            else -> null
        }
    }
    
    override fun extractReference(message: String): String? {
        // Kotak specific UPI reference pattern
        val upiRefPattern = Regex("UPI\\s+Ref\\s+([0-9]+)", RegexOption.IGNORE_CASE)
        upiRefPattern.find(message)?.let { match ->
            return match.groupValues[1].trim()
        }
        
        // Fall back to generic extraction
        return super.extractReference(message)
    }
    
    override fun extractAccountLast4(message: String): String? {
        // Kotak specific pattern: "AC X0000" or "AC XXXX0000"
        val kotakAccountPattern = Regex("AC\\s+[X*]*([0-9]{4})(?:\\s|,|\\.)", RegexOption.IGNORE_CASE)
        kotakAccountPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        return super.extractAccountLast4(message)
    }
    
    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Skip fraud warning links
        if (lowerMessage.contains("not you") && lowerMessage.contains("fraud")) {
            // This is still a transaction message, just with fraud warning
            // Continue processing
        }
        
        // Skip OTP and promotional messages
        if (lowerMessage.contains("otp") || 
            lowerMessage.contains("one time password") ||
            lowerMessage.contains("verification code") ||
            lowerMessage.contains("offer") || 
            lowerMessage.contains("discount") ||
            lowerMessage.contains("cashback offer") ||
            lowerMessage.contains("win ")) {
            return false
        }
        
        // Skip payment request messages
        if (lowerMessage.contains("has requested") || 
            lowerMessage.contains("payment request") ||
            lowerMessage.contains("collect request") ||
            lowerMessage.contains("requesting payment") ||
            lowerMessage.contains("requests rs") ||
            lowerMessage.contains("ignore if already paid")) {
            return false
        }
        
        // Kotak specific transaction keywords
        val kotakTransactionKeywords = listOf(
            "sent", // Kotak uses "Sent Rs.X from Kotak Bank"
            "debited", "credited", "withdrawn", "deposited",
            "spent", "received", "transferred", "paid"
        )
        
        return kotakTransactionKeywords.any { lowerMessage.contains(it) }
    }
}