package com.pennywiseai.tracker.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.repository.CategoryRepository
import com.pennywiseai.tracker.data.repository.MerchantMappingRepository
import com.pennywiseai.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val categoryRepository: CategoryRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    
    private val _transaction = MutableStateFlow<TransactionEntity?>(null)
    val transaction: StateFlow<TransactionEntity?> = _transaction.asStateFlow()
    
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    
    private val _editableTransaction = MutableStateFlow<TransactionEntity?>(null)
    val editableTransaction: StateFlow<TransactionEntity?> = _editableTransaction.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _applyToAllFromMerchant = MutableStateFlow(false)
    val applyToAllFromMerchant: StateFlow<Boolean> = _applyToAllFromMerchant.asStateFlow()
    
    private val _updateExistingTransactions = MutableStateFlow(false)
    val updateExistingTransactions: StateFlow<Boolean> = _updateExistingTransactions.asStateFlow()
    
    private val _existingTransactionCount = MutableStateFlow(0)
    val existingTransactionCount: StateFlow<Int> = _existingTransactionCount.asStateFlow()
    
    // Categories should be based on transaction type
    val categories: StateFlow<List<CategoryEntity>> = combine(
        _editableTransaction,
        _transaction
    ) { editable, original ->
        val transaction = editable ?: original
        transaction?.transactionType == TransactionType.INCOME
    }.flatMapLatest { isIncome ->
        if (isIncome) {
            categoryRepository.getIncomeCategories()
        } else {
            categoryRepository.getExpenseCategories()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            _transaction.value = transactionRepository.getTransactionById(transactionId)
        }
    }
    
    fun enterEditMode() {
        _editableTransaction.value = _transaction.value?.copy()
        _isEditMode.value = true
        _errorMessage.value = null
        
        // Load count of other transactions from same merchant
        _transaction.value?.let { txn ->
            viewModelScope.launch {
                val count = transactionRepository.getOtherTransactionCountForMerchant(
                    txn.merchantName,
                    txn.id
                )
                _existingTransactionCount.value = count
            }
        }
    }
    
    fun exitEditMode() {
        _editableTransaction.value = null
        _isEditMode.value = false
        _errorMessage.value = null
        _applyToAllFromMerchant.value = false
        _updateExistingTransactions.value = false
        _existingTransactionCount.value = 0
    }
    
    fun toggleApplyToAllFromMerchant() {
        _applyToAllFromMerchant.value = !_applyToAllFromMerchant.value
    }
    
    fun toggleUpdateExistingTransactions() {
        _updateExistingTransactions.value = !_updateExistingTransactions.value
    }
    
    fun updateMerchantName(name: String) {
        _editableTransaction.update { current ->
            current?.copy(merchantName = name)
        }
        validateMerchantName(name)
    }
    
    fun updateAmount(amountStr: String) {
        val amount = amountStr.toBigDecimalOrNull()
        if (amount != null && amount > BigDecimal.ZERO) {
            _editableTransaction.update { current ->
                current?.copy(amount = amount)
            }
            _errorMessage.value = null
        } else if (amountStr.isNotEmpty()) {
            _errorMessage.value = "Amount must be a positive number"
        }
    }
    
    fun updateTransactionType(type: TransactionType) {
        _editableTransaction.update { current ->
            current?.copy(transactionType = type)
        }
    }
    
    fun updateCategory(category: String) {
        _editableTransaction.update { current ->
            current?.copy(category = category.ifEmpty { "Others" })
        }
    }
    
    fun updateDateTime(dateTime: LocalDateTime) {
        _editableTransaction.update { current ->
            current?.copy(dateTime = dateTime)
        }
    }
    
    fun updateDescription(description: String?) {
        _editableTransaction.update { current ->
            current?.copy(description = if (description.isNullOrEmpty()) null else description)
        }
    }
    
    fun updateRecurringStatus(isRecurring: Boolean) {
        _editableTransaction.update { current ->
            current?.copy(isRecurring = isRecurring)
        }
    }
    
    fun saveChanges() {
        val toSave = _editableTransaction.value ?: return
        
        // Validate before saving
        if (toSave.merchantName.isBlank()) {
            _errorMessage.value = "Merchant name is required"
            return
        }
        
        if (toSave.amount <= BigDecimal.ZERO) {
            _errorMessage.value = "Amount must be positive"
            return
        }
        
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Normalize merchant name before saving
                val normalizedTransaction = toSave.copy(
                    merchantName = normalizeMerchantName(toSave.merchantName)
                )
                
                transactionRepository.updateTransaction(normalizedTransaction)
                
                // Save merchant mapping if checkbox is checked
                if (_applyToAllFromMerchant.value) {
                    merchantMappingRepository.setMapping(
                        normalizedTransaction.merchantName,
                        normalizedTransaction.category
                    )
                }
                
                // Update existing transactions if checkbox is checked
                if (_updateExistingTransactions.value) {
                    transactionRepository.updateCategoryForMerchant(
                        normalizedTransaction.merchantName,
                        normalizedTransaction.category
                    )
                }
                
                _transaction.value = normalizedTransaction
                _saveSuccess.value = true
                _isEditMode.value = false
                _editableTransaction.value = null
                _errorMessage.value = null
                _applyToAllFromMerchant.value = false
                _updateExistingTransactions.value = false
                _existingTransactionCount.value = 0
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save changes: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    fun cancelEdit() {
        exitEditMode()
    }
    
    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
    
    private fun validateMerchantName(name: String) {
        if (name.isBlank()) {
            _errorMessage.value = "Merchant name is required"
        } else {
            _errorMessage.value = null
        }
    }
    
    /**
     * Normalizes merchant name to consistent format.
     * Converts all-caps to proper case, preserves already mixed case.
     */
    private fun normalizeMerchantName(name: String): String {
        val trimmed = name.trim()
        
        // If it's all uppercase, convert to proper case
        return if (trimmed == trimmed.uppercase()) {
            trimmed.lowercase().split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
        } else {
            // Already has mixed case, keep as is
            trimmed
        }
    }
    
    fun getReportUrl(): String {
        val txn = _transaction.value ?: return ""
        
        // Use the original SMS body if available
        val smsBody = txn.smsBody ?: "Transaction: ${txn.merchantName} - ${txn.amount}"
        
        // Use the original SMS sender if available
        val sender = txn.smsSender ?: ""
        
        android.util.Log.d("TransactionDetailVM", "Generating report URL for transaction:")
        android.util.Log.d("TransactionDetailVM", "SMS Body: ${smsBody.take(100)}...")
        android.util.Log.d("TransactionDetailVM", "SMS Sender: $sender")
        android.util.Log.d("TransactionDetailVM", "Bank Name: ${txn.bankName}")
        
        // URL encode the parameters
        val encodedMessage = java.net.URLEncoder.encode(smsBody, "UTF-8")
        val encodedSender = java.net.URLEncoder.encode(sender, "UTF-8")
        
        // Encrypt device data for verification
        val encryptedDeviceData = com.pennywiseai.tracker.utils.DeviceEncryption.encryptDeviceData(context)
        val encodedDeviceData = if (encryptedDeviceData != null) {
            java.net.URLEncoder.encode(encryptedDeviceData, "UTF-8")
        } else {
            ""
        }
        
        // Create the report URL using hash fragment for privacy
        val url = "https://pennywise-5qh.pages.dev/#message=$encodedMessage&sender=$encodedSender&device=$encodedDeviceData&autoparse=true"
        android.util.Log.d("TransactionDetailVM", "Report URL: ${url.take(200)}...")
        
        return url
    }
}
