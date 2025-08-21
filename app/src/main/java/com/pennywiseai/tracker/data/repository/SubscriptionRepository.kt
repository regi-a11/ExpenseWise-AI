package com.pennywiseai.tracker.data.repository

import com.pennywiseai.tracker.data.database.dao.SubscriptionDao
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.database.entity.SubscriptionState
import com.pennywiseai.tracker.data.parser.bank.HDFCBankParser
import com.pennywiseai.tracker.data.parser.bank.IndianBankParser
import com.pennywiseai.tracker.data.parser.bank.SBIBankParser
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class SubscriptionRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao
) {
    
    companion object {
        private const val TAG = "SubscriptionRepository"
    }
    
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>> = 
        subscriptionDao.getAllSubscriptions()
    
    fun getActiveSubscriptions(): Flow<List<SubscriptionEntity>> = 
        subscriptionDao.getActiveSubscriptions()
    
    fun getUpcomingSubscriptions(daysAhead: Int = 7): Flow<List<SubscriptionEntity>> {
        val futureDate = LocalDate.now().plusDays(daysAhead.toLong())
        return subscriptionDao.getUpcomingSubscriptions(futureDate)
    }
    
    suspend fun getSubscriptionById(id: Long): SubscriptionEntity? = 
        subscriptionDao.getSubscriptionById(id)
    
    suspend fun insertSubscription(subscription: SubscriptionEntity): Long = 
        subscriptionDao.insertSubscription(subscription)
    
    suspend fun updateSubscription(subscription: SubscriptionEntity) = 
        subscriptionDao.updateSubscription(subscription)
    
    suspend fun updateSubscriptionState(id: Long, state: SubscriptionState) = 
        subscriptionDao.updateSubscriptionState(id, state)
    
    suspend fun hideSubscription(id: Long) {
        Log.d(TAG, "Hiding subscription with ID: $id")
        updateSubscriptionState(id, SubscriptionState.HIDDEN)
    }
    
    suspend fun unhideSubscription(id: Long) = 
        updateSubscriptionState(id, SubscriptionState.ACTIVE)
    
    suspend fun deleteSubscription(id: Long) = 
        subscriptionDao.deleteSubscriptionById(id)
    
    /**
     * Creates or updates a subscription from E-Mandate info
     */
    suspend fun createOrUpdateFromEMandate(
        eMandateInfo: HDFCBankParser.EMandateInfo,
        bankName: String,
        smsBody: String? = null
    ): Long {
        val nextPaymentDate = eMandateInfo.nextDeductionDate?.let { dateStr ->
            try {
                // Parse DD/MM/YY format
                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yy"))
            } catch (e: Exception) {
                // Fallback to 30 days from now if parsing fails
                LocalDate.now().plusDays(30)
            }
        } ?: LocalDate.now().plusDays(30)
        
        // For HDFC, UMN is the primary identifier when available
        val existing = if (bankName == "HDFC Bank" && eMandateInfo.umn != null) {
            subscriptionDao.getSubscriptionByUmn(eMandateInfo.umn)
        } else {
            // For other banks or when no UMN, use merchant and amount matching
            subscriptionDao.getSubscriptionByMerchantAndAmount(
                eMandateInfo.merchant,
                eMandateInfo.amount
            )
        }
        
        Log.d(TAG, "E-Mandate lookup - Merchant: ${eMandateInfo.merchant}, Amount: ${eMandateInfo.amount}, " +
                  "Next Date: $nextPaymentDate, Existing: ${existing?.let { "ID=${it.id}, State=${it.state}, " +
                  "StoredDate=${it.nextPaymentDate}" } ?: "NOT FOUND"}")
        
        val subscription = if (existing != null) {
            // Check if this is a hidden subscription that should be reactivated
            // Only reactivate if the new payment date is LATER than the stored date
            val shouldReactivate = existing.state == SubscriptionState.HIDDEN && 
                                  nextPaymentDate.isAfter(existing.nextPaymentDate) &&
                                  nextPaymentDate.isAfter(LocalDate.now())
            
            Log.d(TAG, "Subscription state check - Hidden: ${existing.state == SubscriptionState.HIDDEN}, " +
                      "New date after stored: ${nextPaymentDate.isAfter(existing.nextPaymentDate)}, " +
                      "New date is future: ${nextPaymentDate.isAfter(LocalDate.now())}, " +
                      "Should reactivate: $shouldReactivate")
            
            // If hidden and payment date hasn't changed, don't update
            if (existing.state == SubscriptionState.HIDDEN && !shouldReactivate) {
                // Return the existing ID without any updates
                Log.d(TAG, "Subscription ${existing.id} is HIDDEN and won't be reactivated " +
                          "(payment date not newer). Skipping update.")
                return existing.id
            }
            
            // Update existing subscription (reactivate if needed)
            if (shouldReactivate) {
                Log.i(TAG, "REACTIVATING subscription ${existing.id} - ${existing.merchantName} " +
                          "(old date: ${existing.nextPaymentDate}, new date: $nextPaymentDate)")
            }
            existing.copy(
                amount = eMandateInfo.amount,
                nextPaymentDate = nextPaymentDate,
                merchantName = eMandateInfo.merchant,
                umn = eMandateInfo.umn ?: existing.umn, // Update UMN if provided
                state = if (shouldReactivate) SubscriptionState.ACTIVE else existing.state,
                smsBody = smsBody ?: existing.smsBody, // Update SMS body if provided
                updatedAt = java.time.LocalDateTime.now()
            )
        } else {
            // Create new subscription
            Log.d(TAG, "Creating NEW subscription - Merchant: ${eMandateInfo.merchant}, " +
                      "Amount: ${eMandateInfo.amount}, Date: $nextPaymentDate")
            SubscriptionEntity(
                merchantName = eMandateInfo.merchant,
                amount = eMandateInfo.amount,
                nextPaymentDate = nextPaymentDate,
                state = SubscriptionState.ACTIVE,
                bankName = bankName,
                umn = eMandateInfo.umn,
                category = determineCategory(eMandateInfo.merchant),
                smsBody = smsBody
            )
        }
        
        return subscriptionDao.insertSubscription(subscription)
    }
    
    /**
     * Checks if a transaction matches any active subscription
     */
    suspend fun matchTransactionToSubscription(
        merchantName: String,
        amount: BigDecimal
    ): SubscriptionEntity? {
        val activeSubscription = subscriptionDao.getActiveSubscriptionByMerchant(merchantName)
        
        // Check if amounts match (with some tolerance for small variations)
        return if (activeSubscription != null && 
                   areAmountsEqual(activeSubscription.amount, amount)) {
            activeSubscription
        } else {
            null
        }
    }
    
    /**
     * Updates the next payment date after a subscription charge
     */
    suspend fun updateNextPaymentDateAfterCharge(
        subscriptionId: Long,
        chargeDate: LocalDate = LocalDate.now()
    ) {
        // Assume monthly subscription, add 30 days
        val nextDate = chargeDate.plusDays(30)
        subscriptionDao.updateNextPaymentDate(subscriptionId, nextDate)
    }
    
    
    private fun areAmountsEqual(amount1: BigDecimal, amount2: BigDecimal): Boolean {
        // Allow for small variations (up to 5%)
        val tolerance = amount1.multiply(BigDecimal("0.05"))
        val diff = amount1.subtract(amount2).abs()
        return diff <= tolerance
    }
    
    /**
     * Creates or updates a subscription from Indian Bank Mandate info
     */
    suspend fun createOrUpdateFromIndianBankMandate(
        mandateInfo: IndianBankParser.MandateInfo,
        bankName: String,
        smsBody: String? = null
    ): Long {
        val nextPaymentDate = mandateInfo.nextDeductionDate?.let { dateStr ->
            try {
                // Parse dd-MMM-yy format
                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("d-MMM-yy"))
            } catch (e: Exception) {
                // Fallback to 30 days from now if parsing fails
                LocalDate.now().plusDays(30)
            }
        } ?: LocalDate.now().plusDays(30)
        
        // First try to find by merchant and amount (ignoring date)
        val existing = subscriptionDao.getSubscriptionByMerchantAndAmount(
            mandateInfo.merchant,
            mandateInfo.amount
        )
        
        Log.d(TAG, "Indian Bank Mandate lookup - Merchant: ${mandateInfo.merchant}, Amount: ${mandateInfo.amount}, " +
                  "Next Date: $nextPaymentDate, Existing: ${existing?.let { "ID=${it.id}, State=${it.state}, " +
                  "StoredDate=${it.nextPaymentDate}" } ?: "NOT FOUND"}")
        
        val subscription = if (existing != null) {
            // Check if this is a hidden subscription that should be reactivated
            // Only reactivate if the new payment date is LATER than the stored date
            val shouldReactivate = existing.state == SubscriptionState.HIDDEN && 
                                  nextPaymentDate.isAfter(existing.nextPaymentDate) &&
                                  nextPaymentDate.isAfter(LocalDate.now())
            
            Log.d(TAG, "Subscription state check - Hidden: ${existing.state == SubscriptionState.HIDDEN}, " +
                      "New date after stored: ${nextPaymentDate.isAfter(existing.nextPaymentDate)}, " +
                      "New date is future: ${nextPaymentDate.isAfter(LocalDate.now())}, " +
                      "Should reactivate: $shouldReactivate")
            
            // If hidden and payment date hasn't changed, don't update
            if (existing.state == SubscriptionState.HIDDEN && !shouldReactivate) {
                // Return the existing ID without any updates
                Log.d(TAG, "Subscription ${existing.id} is HIDDEN and won't be reactivated " +
                          "(payment date not newer). Skipping update.")
                return existing.id
            }
            
            // Update existing subscription (reactivate if needed)
            if (shouldReactivate) {
                Log.i(TAG, "REACTIVATING subscription ${existing.id} - ${existing.merchantName} " +
                          "(old date: ${existing.nextPaymentDate}, new date: $nextPaymentDate)")
            }
            existing.copy(
                amount = mandateInfo.amount,
                nextPaymentDate = nextPaymentDate,
                merchantName = mandateInfo.merchant,
                state = if (shouldReactivate) SubscriptionState.ACTIVE else existing.state,
                smsBody = smsBody ?: existing.smsBody,
                updatedAt = java.time.LocalDateTime.now()
            )
        } else {
            // Create new subscription
            Log.d(TAG, "Creating NEW subscription - Merchant: ${mandateInfo.merchant}, " +
                      "Amount: ${mandateInfo.amount}, Date: $nextPaymentDate")
            SubscriptionEntity(
                merchantName = mandateInfo.merchant,
                amount = mandateInfo.amount,
                nextPaymentDate = nextPaymentDate,
                state = SubscriptionState.ACTIVE,
                bankName = bankName,
                category = determineCategory(mandateInfo.merchant),
                smsBody = smsBody
            )
        }
        
        return subscriptionDao.insertSubscription(subscription)
    }
    
    /**
     * Creates or updates a subscription from SBI UPI-Mandate info
     */
    suspend fun createOrUpdateFromSBIMandate(
        upiMandateInfo: SBIBankParser.UPIMandateInfo,
        bankName: String,
        smsBody: String? = null
    ): Long {
        val nextPaymentDate = upiMandateInfo.nextDeductionDate?.let { dateStr ->
            try {
                // Parse DD/MM/YY format if provided
                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yy"))
            } catch (e: Exception) {
                // Fallback to 30 days from now if parsing fails
                LocalDate.now().plusDays(30)
            }
        } ?: LocalDate.now().plusDays(30) // SBI doesn't provide date, default to 30 days
        
        // For SBI, UMN is the primary identifier when available
        val existing = if (upiMandateInfo.umn != null) {
            subscriptionDao.getSubscriptionByUmn(upiMandateInfo.umn)
        } else {
            // When no UMN, use merchant and amount matching
            subscriptionDao.getSubscriptionByMerchantAndAmount(
                upiMandateInfo.merchant,
                upiMandateInfo.amount
            )
        }
        
        Log.d(TAG, "SBI UPI-Mandate lookup - Merchant: ${upiMandateInfo.merchant}, Amount: ${upiMandateInfo.amount}, " +
                  "UMN: ${upiMandateInfo.umn}, Existing: ${existing?.let { "ID=${it.id}, State=${it.state}" } ?: "NOT FOUND"}")
        
        val subscription = if (existing != null) {
            // Check if this is a hidden subscription that should be reactivated
            val newState = if (existing.state == SubscriptionState.HIDDEN) {
                SubscriptionState.ACTIVE
            } else {
                existing.state
            }
            
            existing.copy(
                amount = upiMandateInfo.amount,
                nextPaymentDate = nextPaymentDate,
                umn = upiMandateInfo.umn ?: existing.umn,
                state = newState,
                smsBody = smsBody ?: existing.smsBody,
                updatedAt = java.time.LocalDateTime.now()
            ).also {
                subscriptionDao.updateSubscription(it)
                Log.d(TAG, "Updated existing SBI subscription ID=${it.id}")
            }
        } else {
            // Create new subscription
            SubscriptionEntity(
                merchantName = upiMandateInfo.merchant,
                amount = upiMandateInfo.amount,
                nextPaymentDate = nextPaymentDate,
                state = SubscriptionState.ACTIVE,
                bankName = bankName,
                umn = upiMandateInfo.umn,
                category = determineCategory(upiMandateInfo.merchant),
                smsBody = smsBody
            )
        }
        
        return subscriptionDao.insertSubscription(subscription)
    }
    
    private fun determineCategory(merchantName: String): String {
        // Use unified category mapping
        return CategoryMapping.getCategory(merchantName)
    }
}