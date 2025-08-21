package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.parser.ParsedTransaction
import java.math.BigDecimal

/**
 * Parser for Juspay/Amazon Pay wallet transactions.
 * Handles messages from XX-JUSPAY-X and similar senders.
 */
class JuspayParser : BankParser() {
    
    override fun getBankName() = "Amazon Pay"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("JUSPAY")
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // For Juspay/Amazon Pay, the merchant info is usually not in the message
        // These are typically payment gateway transactions
        
        // Check for common patterns
        val lowerMessage = message.lowercase()
        return when {
            lowerMessage.contains("amazon") -> "Amazon"
            lowerMessage.contains("apay wallet") -> "Amazon Pay Transaction"
            lowerMessage.contains("wallet") -> "Amazon Pay Transaction"
            else -> super.extractMerchant(message, sender) ?: "Amazon Pay"
        }
    }
    
    override fun extractTransactionType(message: String): TransactionType {
        // Juspay/Amazon Pay transactions are typically wallet top-ups
        // charged to credit cards, so we treat them as credit transactions
        return TransactionType.CREDIT
    }
}