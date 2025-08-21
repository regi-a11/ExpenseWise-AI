package com.pennywiseai.tracker.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.repository.CategoryRepository
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.presentation.common.getDateRangeForPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()
    
    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()
    
    private val _transactionTypeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    val transactionTypeFilter: StateFlow<TransactionTypeFilter> = _transactionTypeFilter.asStateFlow()
    
    private val _sortOption = MutableStateFlow(SortOption.DATE_NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()
    
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    
    private val _filteredTotals = MutableStateFlow(FilteredTotals())
    val filteredTotals: StateFlow<FilteredTotals> = _filteredTotals.asStateFlow()
    
    private val _deletedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val deletedTransaction: StateFlow<TransactionEntity?> = _deletedTransaction.asStateFlow()
    
    // Categories flow - will be used to map category names to colors
    val categories: StateFlow<Map<String, CategoryEntity>> = categoryRepository.getAllCategories()
        .map { categoryList ->
            categoryList.associateBy { it.name }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    
    init {
        // Combine all filters: search query, period, category, transaction type, and sort
        combine(
            searchQuery.debounce(300), // Debounce search for performance
            selectedPeriod,
            categoryFilter,
            transactionTypeFilter,
            sortOption
        ) { query, period, category, typeFilter, sort ->
            FilterParams(query, period, category, typeFilter) to sort
        }.flatMapLatest { (params, sort) ->
            getFilteredTransactions(params.query, params.period, params.category, params.typeFilter)
                .map { transactions -> sortTransactions(transactions, sort) }
        }.onEach { transactions ->
            _uiState.value = _uiState.value.copy(
                transactions = transactions,
                groupedTransactions = groupTransactionsByDate(transactions),
                isLoading = false
            )
            // Calculate totals for filtered transactions
            _filteredTotals.value = calculateTotals(transactions)
        }.launchIn(viewModelScope)
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }
    
    fun setCategoryFilter(category: String) {
        println("DEBUG: Setting category filter to: '$category'")
        _categoryFilter.value = category
    }
    
    fun clearCategoryFilter() {
        _categoryFilter.value = null
    }
    
    fun setTransactionTypeFilter(filter: TransactionTypeFilter) {
        _transactionTypeFilter.value = filter
    }
    
    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }
    
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            _deletedTransaction.value = transaction
            transactionRepository.softDeleteTransaction(transaction)
        }
    }
    
    fun undoDelete() {
        _deletedTransaction.value?.let { transaction ->
            viewModelScope.launch {
                transactionRepository.undoDeleteTransaction(transaction)
                _deletedTransaction.value = null
            }
        }
    }
    
    fun undoDeleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.undoDeleteTransaction(transaction)
        }
    }
    
    fun clearDeletedTransaction() {
        _deletedTransaction.value = null
    }
    
    private fun getFilteredTransactions(
        searchQuery: String,
        period: TimePeriod,
        category: String?,
        typeFilter: TransactionTypeFilter
    ): Flow<List<TransactionEntity>> {
        // Start with the base flow based on category filter
        val baseFlow = if (category != null) {
            println("DEBUG: Filtering by category: '$category'")
            transactionRepository.getTransactionsByCategory(category)
        } else {
            transactionRepository.getAllTransactions()
        }
        
        // Apply period filter
        val periodFilteredFlow = when (period) {
            TimePeriod.ALL -> baseFlow
            else -> {
                val (startDate, endDate) = getDateRangeForPeriod(period)
                val startDateTime = startDate.atStartOfDay()
                val endDateTime = endDate.atTime(23, 59, 59)
                
                baseFlow.map { transactions ->
                    transactions.filter { it.dateTime in startDateTime..endDateTime }
                }
            }
        }
        
        // Apply transaction type filter
        val typeFilteredFlow = periodFilteredFlow.map { transactions ->
            when (typeFilter) {
                TransactionTypeFilter.ALL -> transactions
                TransactionTypeFilter.INCOME -> transactions.filter { it.transactionType == TransactionType.INCOME }
                TransactionTypeFilter.EXPENSE -> transactions.filter { it.transactionType == TransactionType.EXPENSE }
                TransactionTypeFilter.CREDIT -> transactions.filter { it.transactionType == TransactionType.CREDIT }
                TransactionTypeFilter.TRANSFER -> transactions.filter { it.transactionType == TransactionType.TRANSFER }
                TransactionTypeFilter.INVESTMENT -> transactions.filter { it.transactionType == TransactionType.INVESTMENT }
            }
        }
        
        // Apply search filter
        return if (searchQuery.isBlank()) {
            typeFilteredFlow
        } else {
            typeFilteredFlow.map { transactions ->
                transactions.filter { transaction ->
                    transaction.merchantName.contains(searchQuery, ignoreCase = true) ||
                    transaction.description?.contains(searchQuery, ignoreCase = true) == true
                }
            }
        }
    }
    
    private fun sortTransactions(transactions: List<TransactionEntity>, sortOption: SortOption): List<TransactionEntity> {
        return when (sortOption) {
            SortOption.DATE_NEWEST -> transactions.sortedByDescending { it.dateTime }
            SortOption.DATE_OLDEST -> transactions.sortedBy { it.dateTime }
            SortOption.AMOUNT_HIGHEST -> transactions.sortedByDescending { it.amount }
            SortOption.AMOUNT_LOWEST -> transactions.sortedBy { it.amount }
            SortOption.MERCHANT_AZ -> transactions.sortedBy { it.merchantName.lowercase() }
            SortOption.MERCHANT_ZA -> transactions.sortedByDescending { it.merchantName.lowercase() }
        }
    }
    
    private fun groupTransactionsByDate(
        transactions: List<TransactionEntity>
    ): Map<DateGroup, List<TransactionEntity>> {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val weekStart = today.minusWeeks(1)
        
        return transactions.groupBy { transaction ->
            val transactionDate = transaction.dateTime.toLocalDate()
            when {
                transactionDate == today -> DateGroup.TODAY
                transactionDate == yesterday -> DateGroup.YESTERDAY
                transactionDate > weekStart -> DateGroup.THIS_WEEK
                else -> DateGroup.EARLIER
            }
        }
    }
    
    private fun calculateTotals(transactions: List<TransactionEntity>): FilteredTotals {
        val income = transactions
            .filter { it.transactionType == TransactionType.INCOME }
            .sumOf { it.amount.toDouble() }
            .toBigDecimal()
            
        val expenses = transactions
            .filter { it.transactionType == TransactionType.EXPENSE }
            .sumOf { it.amount.toDouble() }
            .toBigDecimal()
            
        val credit = transactions
            .filter { it.transactionType == TransactionType.CREDIT }
            .sumOf { it.amount.toDouble() }
            .toBigDecimal()
            
        val transfer = transactions
            .filter { it.transactionType == TransactionType.TRANSFER }
            .sumOf { it.amount.toDouble() }
            .toBigDecimal()
            
        val investment = transactions
            .filter { it.transactionType == TransactionType.INVESTMENT }
            .sumOf { it.amount.toDouble() }
            .toBigDecimal()
            
        // Calculate net balance (income minus all outgoing)
        val netBalance = income - expenses - credit - transfer - investment
        
        return FilteredTotals(
            income = income,
            expenses = expenses,
            credit = credit,
            transfer = transfer,
            investment = investment,
            netBalance = netBalance,
            transactionCount = transactions.size
        )
    }
    
    fun getReportUrl(transaction: TransactionEntity): String {
        // If we have the original SMS body, create report URL
        val smsBody = transaction.smsBody ?: ""
        // Use the original SMS sender if available
        val sender = transaction.smsSender ?: ""
        
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
        return "https://pennywise-5qh.pages.dev/#message=$encodedMessage&sender=$encodedSender&device=$encodedDeviceData&autoparse=true"
    }
    
}

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val groupedTransactions: Map<DateGroup, List<TransactionEntity>> = emptyMap(),
    val isLoading: Boolean = true
)

data class FilterParams(
    val query: String,
    val period: TimePeriod,
    val category: String?,
    val typeFilter: TransactionTypeFilter
)

enum class DateGroup(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    EARLIER("Earlier")
}

enum class SortOption(val label: String) {
    DATE_NEWEST("Newest First"),
    DATE_OLDEST("Oldest First"),
    AMOUNT_HIGHEST("Highest Amount"),
    AMOUNT_LOWEST("Lowest Amount"),
    MERCHANT_AZ("Merchant (A-Z)"),
    MERCHANT_ZA("Merchant (Z-A)")
}

data class FilteredTotals(
    val income: BigDecimal = BigDecimal.ZERO,
    val expenses: BigDecimal = BigDecimal.ZERO,
    val credit: BigDecimal = BigDecimal.ZERO,
    val transfer: BigDecimal = BigDecimal.ZERO,
    val investment: BigDecimal = BigDecimal.ZERO,
    val netBalance: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0
)
