package com.pennywiseai.tracker.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.presentation.common.getDateRangeForPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()
    
    private val _transactionTypeFilter = MutableStateFlow(TransactionTypeFilter.EXPENSE)
    val transactionTypeFilter: StateFlow<TransactionTypeFilter> = _transactionTypeFilter.asStateFlow()
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    init {
        loadAnalytics()
    }
    
    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
        loadAnalytics()
    }
    
    fun setTransactionTypeFilter(filter: TransactionTypeFilter) {
        _transactionTypeFilter.value = filter
        loadAnalytics()
    }
    
    private fun loadAnalytics() {
        viewModelScope.launch {
            val dateRange = getDateRangeForPeriod(_selectedPeriod.value)
            
            transactionRepository.getTransactionsBetweenDates(
                startDate = dateRange.first,
                endDate = dateRange.second
            ).collect { transactions ->
                // Filter by selected transaction type
                val filteredTransactions = when (_transactionTypeFilter.value) {
                    TransactionTypeFilter.ALL -> transactions
                    TransactionTypeFilter.INCOME -> transactions.filter { 
                        it.transactionType == com.pennywiseai.tracker.data.database.entity.TransactionType.INCOME 
                    }
                    TransactionTypeFilter.EXPENSE -> transactions.filter { 
                        it.transactionType == com.pennywiseai.tracker.data.database.entity.TransactionType.EXPENSE 
                    }
                    TransactionTypeFilter.CREDIT -> transactions.filter { 
                        it.transactionType == com.pennywiseai.tracker.data.database.entity.TransactionType.CREDIT 
                    }
                    TransactionTypeFilter.TRANSFER -> transactions.filter { 
                        it.transactionType == com.pennywiseai.tracker.data.database.entity.TransactionType.TRANSFER 
                    }
                    TransactionTypeFilter.INVESTMENT -> transactions.filter { 
                        it.transactionType == com.pennywiseai.tracker.data.database.entity.TransactionType.INVESTMENT 
                    }
                }
                
                // Calculate total
                val totalSpending = filteredTransactions.sumOf { it.amount.toDouble() }.toBigDecimal()
                
                // Group by category
                val categoryBreakdown = filteredTransactions
                    .groupBy { it.category ?: "Others" }
                    .map { (categoryName, txns) -> 
                        val categoryTotal = txns.sumOf { it.amount.toDouble() }.toBigDecimal()
                        CategoryData(
                            name = categoryName,
                            amount = categoryTotal,
                            percentage = if (totalSpending > BigDecimal.ZERO) {
                                (categoryTotal.divide(totalSpending, 4, java.math.RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
                            } else 0f,
                            transactionCount = txns.size
                        )
                    }
                    .sortedByDescending { it.amount }
                
                // Group by merchant
                val merchantBreakdown = filteredTransactions
                    .groupBy { it.merchantName }
                    .mapValues { (merchant, txns) -> 
                        MerchantData(
                            name = merchant,
                            amount = txns.sumOf { it.amount.toDouble() }.toBigDecimal(),
                            transactionCount = txns.size,
                            isSubscription = txns.any { it.isRecurring }
                        )
                    }
                    .values
                    .sortedByDescending { it.amount }
                    .take(10) // Top 10 merchants
                
                // Calculate average amount
                val averageAmount = if (filteredTransactions.isNotEmpty()) {
                    totalSpending.divide(BigDecimal(filteredTransactions.size), 2, java.math.RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }
                
                // Get top category info
                val topCategory = categoryBreakdown.firstOrNull()
                
                _uiState.value = AnalyticsUiState(
                    totalSpending = totalSpending,
                    categoryBreakdown = categoryBreakdown,
                    topMerchants = merchantBreakdown,
                    transactionCount = filteredTransactions.size,
                    averageAmount = averageAmount,
                    topCategory = topCategory?.name,
                    topCategoryPercentage = topCategory?.percentage ?: 0f,
                    isLoading = false
                )
            }
        }
    }
    
}

data class AnalyticsUiState(
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val categoryBreakdown: List<CategoryData> = emptyList(),
    val topMerchants: List<MerchantData> = emptyList(),
    val transactionCount: Int = 0,
    val averageAmount: BigDecimal = BigDecimal.ZERO,
    val topCategory: String? = null,
    val topCategoryPercentage: Float = 0f,
    val isLoading: Boolean = true
)

data class CategoryData(
    val name: String,
    val amount: BigDecimal,
    val percentage: Float,
    val transactionCount: Int
)

data class MerchantData(
    val name: String,
    val amount: BigDecimal,
    val transactionCount: Int,
    val isSubscription: Boolean
)

