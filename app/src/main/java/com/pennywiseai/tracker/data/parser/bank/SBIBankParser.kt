package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal

/**
 * Parser for State Bank of India (SBI) SMS messages
 */
class SBIBankParser : BankParser() {
    
    override fun getBankName() = "State Bank of India"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("SBI") || 
               normalizedSender.contains("SBIINB") ||
               normalizedSender.contains("SBIUPI") ||
               normalizedSender.contains("SBICRD") ||
               normalizedSender.contains("ATMSBI") ||
               // Direct sender IDs
               normalizedSender == "SBIBK" ||
               normalizedSender == "SBIBNK" ||
               // DLT patterns for transactions (-S suffix)
               normalizedSender.matches(Regex("^[A-Z]{2}-SBIBK-S$")) ||
               // Other DLT patterns (OTP, Promotional, Govt)
               normalizedSender.matches(Regex("^[A-Z]{2}-SBIBK-[TPG]$")) ||
               // Legacy patterns without suffix
               normalizedSender.matches(Regex("^[A-Z]{2}-SBIBK$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-SBI$"))
    }
    
    override fun extractAmount(message: String): BigDecimal? {
        // Pattern 0: A/C debited by 20.0 (UPI format)
        val upiDebitPattern = Regex("""debited\s+by\s+(\d+(?:,\d{3})*(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        upiDebitPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 0a: A/c credited by Rs.500 (UPI format)
        val upiCreditPattern = Regex("""credited\s+by\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        upiCreditPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 1: Rs 500 debited
        val debitPattern1 = Regex("""Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?debited""", RegexOption.IGNORE_CASE)
        debitPattern1.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 2: INR 500 debited
        val debitPattern2 = Regex("""INR\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?debited""", RegexOption.IGNORE_CASE)
        debitPattern2.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 3: Rs 500 credited
        val creditPattern1 = Regex("""Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?credited""", RegexOption.IGNORE_CASE)
        creditPattern1.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 4: INR 500 credited
        val creditPattern2 = Regex("""INR\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?credited""", RegexOption.IGNORE_CASE)
        creditPattern2.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 5: withdrawn Rs 500
        val withdrawPattern = Regex("""withdrawn\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        withdrawPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 6: transferred Rs 500
        val transferPattern = Regex("""transferred\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        transferPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 7: UPI patterns - "paid to MERCHANT@upi Rs 500"
        val upiPattern = Regex("""paid\s+to\s+[\w.-]+@[\w]+\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        upiPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 8: ATM withdrawal - "ATM withdrawal of Rs 500"
        val atmPattern = Regex("""ATM\s+withdrawal\s+of\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        atmPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 9: YONO Cash withdrawal - "Yono Cash Rs 3000 w/d@SBI ATM"
        val yonoCashPattern = Regex("""Yono\s+Cash\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        yonoCashPattern.find(message)?.let { match ->
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
    
    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        
        // SBI-specific patterns
        return when {
            lowerMessage.contains("withdrawn") -> TransactionType.EXPENSE
            lowerMessage.contains("transferred") -> TransactionType.EXPENSE
            lowerMessage.contains("paid to") -> TransactionType.EXPENSE
            lowerMessage.contains("atm withdrawal") -> TransactionType.EXPENSE
            
            // Fall back to base class for common patterns
            else -> super.extractTransactionType(message)
        }
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // Pattern 0: trf to Merchant (UPI format)
        val trfPattern = Regex("""trf\s+to\s+([^.\n]+?)(?:\s+Ref|\s+ref|$)""", RegexOption.IGNORE_CASE)
        trfPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        // Pattern 0a: transfer from Sender (credit format)
        val transferFromPattern = Regex("""transfer\s+from\s+([^.\n]+?)(?:\s+Ref|\s+ref|$)""", RegexOption.IGNORE_CASE)
        transferFromPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        // Pattern 1: paid to MERCHANT@upi
        val upiMerchantPattern = Regex("""paid\s+to\s+([\w.-]+)@[\w]+""", RegexOption.IGNORE_CASE)
        upiMerchantPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1])
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        // Pattern 2: YONO Cash ATM - "w/d@SBI ATM S1NW000093009"
        val yonoAtmPattern = Regex("""w/d@SBI\s+ATM\s+([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
        yonoAtmPattern.find(message)?.let { match ->
            val atmId = match.groupValues[1]
            return "YONO Cash ATM - $atmId"
        }
        
        // Pattern 2a: Regular ATM location
        val atmPattern = Regex("""ATM\s+(?:withdrawal\s+)?(?:at\s+)?([^.\n]+?)(?:\s+on|\s+Avl)""", RegexOption.IGNORE_CASE)
        atmPattern.find(message)?.let { match ->
            val location = cleanMerchantName(match.groupValues[1])
            if (isValidMerchantName(location)) {
                return "ATM - $location"
            }
        }
        
        // Pattern 3: NEFT/IMPS/RTGS with beneficiary
        val neftPattern = Regex("""(?:NEFT|IMPS|RTGS)[^:]*:\s*([^.\n]+?)(?:\s+Ref|\s+on|$)""", RegexOption.IGNORE_CASE)
        neftPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1])
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        // Fall back to base class patterns
        return super.extractMerchant(message, sender)
    }
    
    override fun extractAccountLast4(message: String): String? {
        // Pattern 1: A/c XNNNN or A/c XXNNNN - extract everything after A/c
        val pattern1 = Regex("""A/c\s+([X\*]*\d+)""", RegexOption.IGNORE_CASE)
        pattern1.find(message)?.let { match ->
            val accountStr = match.groupValues[1]
            // Extract just the digits and take last 4
            val digitsOnly = accountStr.filter { it.isDigit() }
            return if (digitsOnly.length >= 4) {
                digitsOnly.takeLast(4)
            } else {
                digitsOnly
            }
        }
        
        // Pattern 2: from A/c ending 1234
        val pattern2 = Regex("""A/c\s+ending\s+(\d{4})""", RegexOption.IGNORE_CASE)
        pattern2.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Pattern 3: a/c no. XX1234
        val pattern3 = Regex("""a/c\s+no\.?\s+(?:XX|X\*+)?(\d{4})""", RegexOption.IGNORE_CASE)
        pattern3.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Fall back to base class
        return super.extractAccountLast4(message)
    }
    
    override fun extractBalance(message: String): BigDecimal? {
        // Pattern 1: Avl Bal Rs 1000.00
        val pattern1 = Regex("""Avl\s+Bal\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        pattern1.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 2: Available Balance: Rs 1000
        val pattern2 = Regex("""Available\s+Balance:?\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        pattern2.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 3: Bal: Rs 1000
        val pattern3 = Regex("""Bal:?\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        pattern3.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Fall back to base class
        return super.extractBalance(message)
    }
    
    override fun extractReference(message: String): String? {
        // Pattern 1: Ref No 123456789
        val pattern1 = Regex("""Ref\s+No\.?\s*(\w+)""", RegexOption.IGNORE_CASE)
        pattern1.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Pattern 2: Txn# 123456
        val pattern2 = Regex("""Txn#\s*(\w+)""", RegexOption.IGNORE_CASE)
        pattern2.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Pattern 3: transaction ID 123456
        val pattern3 = Regex("""transaction\s+ID:?\s*(\w+)""", RegexOption.IGNORE_CASE)
        pattern3.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Fall back to base class
        return super.extractReference(message)
    }
    
    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Skip e-statement notifications
        if (lowerMessage.contains("e-statement of sbi credit card")) {
            return false
        }
        
        // Skip UPI-Mandate creation notifications
        if (isUPIMandateNotification(message)) {
            return false
        }
        
        // Fall back to base class for other checks
        return super.isTransactionMessage(message)
    }
    
    /**
     * Checks if this is a UPI-Mandate creation notification.
     * Only returns true if it's a mandate creation message (not a debit notification).
     */
    fun isUPIMandateNotification(message: String): Boolean {
        return message.contains("UPI-Mandate", ignoreCase = true) && 
               message.contains("successfully created", ignoreCase = true)
    }
    
    /**
     * Parses UPI-Mandate subscription information.
     * Similar to HDFC's EMandateInfo structure for consistency.
     */
    fun parseUPIMandateSubscription(message: String): UPIMandateInfo? {
        if (!isUPIMandateNotification(message)) {
            return null
        }
        
        // Extract amount - "Rs.1050.00"
        val amountPattern = Regex("""Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        val amount = amountPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        } ?: return null
        
        // Extract merchant - "towards Google Play"
        val merchantPattern = Regex("""towards\s+([^.\n]+?)(?:\s+from|\s+A/c|$)""", RegexOption.IGNORE_CASE)
        val merchant = merchantPattern.find(message)?.let { match ->
            cleanMerchantName(match.groupValues[1].trim())
        } ?: "Unknown Subscription"
        
        // Extract UMN - "UMN:f6b4a989bc9d465984e4e7519a6622f4@okaxis"
        val umnPattern = Regex("""UMN:([^.\s]+)""", RegexOption.IGNORE_CASE)
        val umn = umnPattern.find(message)?.groupValues?.get(1)
        
        return UPIMandateInfo(
            amount = amount,
            nextDeductionDate = null, // SBI doesn't provide next deduction date in creation message
            merchant = merchant,
            umn = umn
        )
    }
    
    /**
     * UPI-Mandate subscription information for SBI.
     * Compatible with HDFC's EMandateInfo structure.
     */
    data class UPIMandateInfo(
        val amount: BigDecimal,
        val nextDeductionDate: String?,
        val merchant: String,
        val umn: String?
    )
}