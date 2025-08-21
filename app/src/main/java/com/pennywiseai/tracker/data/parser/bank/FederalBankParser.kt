package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal

/**
 * Parser for Federal Bank SMS messages
 * 
 * Supported formats:
 * - UPI transactions: "Rs 34.51 debited via UPI on 08-05-2025 13:48:03 to VPA ..."
 * - Card transactions
 * - ATM withdrawals
 * - NEFT/IMPS transfers
 * 
 * Sender patterns: AD-FEDBNK-S, JM-FEDBNK-S, etc.
 */
class FederalBankParser : BankParser() {
    
    override fun getBankName() = "Federal Bank"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("FEDBNK") ||
               normalizedSender.contains("FEDERAL") ||
               normalizedSender.contains("FEDFIB") ||
               // DLT patterns for transactions (-S suffix)
               normalizedSender.matches(Regex("^[A-Z]{2}-FEDBNK-S$")) ||
               // FedFiB patterns
               normalizedSender.matches(Regex("^[A-Z]{2}-FedFiB-[A-Z]$")) ||
               // Other DLT patterns
               normalizedSender.matches(Regex("^[A-Z]{2}-FEDBNK-[TPG]$")) ||
               // Legacy patterns
               normalizedSender.matches(Regex("^[A-Z]{2}-FEDBNK$"))
    }
    
    override fun extractAmount(message: String): BigDecimal? {
        // Pattern 1: Rs 34.51 debited via UPI
        val debitPattern = Regex(
            """Rs\s+([0-9,]+(?:\.\d{2})?)\s+debited""",
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
        
        // Pattern 2: Rs 500.00 credited
        val creditPattern = Regex(
            """Rs\s+([0-9,]+(?:\.\d{2})?)\s+credited""",
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
        
        // Pattern 3: withdrawn Rs 500
        val withdrawnPattern = Regex(
            """withdrawn\s+Rs\s+([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        withdrawnPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Fall back to base class patterns
        return super.extractAmount(message)
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // Pattern 1: UPI transactions - "to VPA merchant@bank"
        if (message.contains("VPA", ignoreCase = true)) {
            val vpaPattern = Regex(
                """to\s+VPA\s+([^.\s]+)""",
                RegexOption.IGNORE_CASE
            )
            vpaPattern.find(message)?.let { match ->
                val vpa = match.groupValues[1].trim()
                return parseUPIMerchant(vpa)
            }
        }
        
        // Pattern 2: "to <merchant name>" (general)
        val toPattern = Regex(
            """to\s+([^.\n]+?)(?:\.\s*Ref|Ref\s+No|$)""",
            RegexOption.IGNORE_CASE
        )
        toPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            // Skip if it's VPA (already handled above)
            if (!merchant.contains("VPA", ignoreCase = true)) {
                val cleaned = cleanMerchantName(merchant)
                if (isValidMerchantName(cleaned)) {
                    return cleaned
                }
            }
        }
        
        // Pattern 3: "from <sender name>" (for credits)
        val fromPattern = Regex(
            """from\s+([^.\n]+?)(?:\.\s*|$)""",
            RegexOption.IGNORE_CASE
        )
        fromPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        // Pattern 4: ATM transactions
        if (message.contains("ATM", ignoreCase = true) || 
            message.contains("withdrawn", ignoreCase = true)) {
            return "ATM Withdrawal"
        }
        
        // Fall back to base class extraction
        return super.extractMerchant(message, sender)
    }
    
    private fun parseUPIMerchant(vpa: String): String {
        // Extract merchant name from VPA
        val cleanVPA = vpa.split("@")[0].lowercase()
        
        return when {
            // Ride-hailing
            cleanVPA.contains("uber") -> "Uber"
            cleanVPA.contains("ola") -> "Ola"
            cleanVPA.contains("rapido") -> "Rapido"
            
            // Payment apps
            cleanVPA.contains("paytm") -> "Paytm"
            cleanVPA.contains("bharatpe") -> "BharatPe"
            cleanVPA.contains("phonepe") -> "PhonePe"
            cleanVPA.contains("googlepay") || cleanVPA.contains("gpay") -> "Google Pay"
            
            // E-commerce
            cleanVPA.contains("amazon") -> "Amazon"
            cleanVPA.contains("flipkart") -> "Flipkart"
            cleanVPA.contains("myntra") -> "Myntra"
            cleanVPA.contains("meesho") -> "Meesho"
            
            // Food delivery
            cleanVPA.contains("swiggy") -> "Swiggy"
            cleanVPA.contains("zomato") -> "Zomato"
            
            // Entertainment
            cleanVPA.contains("netflix") -> "Netflix"
            cleanVPA.contains("spotify") -> "Spotify"
            cleanVPA.contains("hotstar") || cleanVPA.contains("disney") -> "Disney+ Hotstar"
            cleanVPA.contains("prime") -> "Amazon Prime"
            cleanVPA.contains("pvr") || cleanVPA.contains("inox") -> "PVR Inox"
            cleanVPA.contains("bookmyshow") || cleanVPA.contains("bms") -> "BookMyShow"
            
            // Telecom
            cleanVPA.contains("jio") -> "Jio"
            cleanVPA.contains("airtel") -> "Airtel"
            cleanVPA.contains("vodafone") || cleanVPA.contains("vi") -> "Vi"
            cleanVPA.contains("bsnl") -> "BSNL"
            
            // Travel
            cleanVPA.contains("irctc") -> "IRCTC"
            cleanVPA.contains("redbus") -> "RedBus"
            cleanVPA.contains("makemytrip") || cleanVPA.contains("mmt") -> "MakeMyTrip"
            cleanVPA.contains("goibibo") -> "Goibibo"
            cleanVPA.contains("oyo") -> "OYO"
            cleanVPA.contains("airbnb") -> "Airbnb"
            
            // Payment gateways - try to extract actual merchant
            cleanVPA.contains("razorpay") || cleanVPA.contains("razorp") || cleanVPA.contains("rzp") -> {
                when {
                    cleanVPA.contains("pvr") -> "PVR"
                    cleanVPA.contains("inox") -> "PVR Inox"
                    cleanVPA.contains("swiggy") -> "Swiggy"
                    cleanVPA.contains("zomato") -> "Zomato"
                    else -> "Online Payment"
                }
            }
            cleanVPA.contains("payu") || cleanVPA.contains("billdesk") || cleanVPA.contains("ccavenue") -> "Online Payment"
            
            // Individual transfers (just numbers)
            cleanVPA.matches(Regex("\\d+")) -> "Individual"
            
            // Default - try to extract meaningful name
            else -> {
                val parts = cleanVPA.split(".", "-", "_")
                parts.firstOrNull { it.length > 3 && !it.all { char -> char.isDigit() } }
                    ?.replaceFirstChar { it.uppercase() } ?: "Merchant"
            }
        }
    }
}