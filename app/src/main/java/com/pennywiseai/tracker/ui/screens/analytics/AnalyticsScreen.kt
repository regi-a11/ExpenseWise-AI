package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.ui.components.*
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToTransactions: (category: String?, merchant: String?, period: String?) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val transactionTypeFilter by viewModel.transactionTypeFilter.collectAsStateWithLifecycle()
    var showAdvancedFilters by remember { mutableStateOf(false) }
    
    // Calculate active filter count
    val activeFilterCount = if (transactionTypeFilter != TransactionTypeFilter.EXPENSE) 1 else 0
    
    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = Dimensions.Padding.content,
            end = Dimensions.Padding.content,
            top = Spacing.md,
            bottom = Dimensions.Component.bottomBarHeight + Spacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Period Selector - Always visible
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(TimePeriod.values().toList()) { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.selectPeriod(period) },
                        label = { Text(period.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
        
        // Collapsible Transaction Type Filter
        item {
            CollapsibleFilterRow(
                isExpanded = showAdvancedFilters,
                activeFilterCount = activeFilterCount,
                onToggle = { showAdvancedFilters = !showAdvancedFilters },
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(TransactionTypeFilter.values().toList()) { typeFilter ->
                        FilterChip(
                            selected = transactionTypeFilter == typeFilter,
                            onClick = { viewModel.setTransactionTypeFilter(typeFilter) },
                            label = { Text(typeFilter.label) },
                            leadingIcon = if (transactionTypeFilter == typeFilter) {
                                {
                                    when (typeFilter) {
                                        TransactionTypeFilter.INCOME -> Icon(
                                            Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.EXPENSE -> Icon(
                                            Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.CREDIT -> Icon(
                                            Icons.Default.CreditCard,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.TRANSFER -> Icon(
                                            Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.INVESTMENT -> Icon(
                                            Icons.Default.ShowChart,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        else -> null
                                    }
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
            }
        }
        
        // Analytics Summary Card
        if (uiState.totalSpending > BigDecimal.ZERO || uiState.transactionCount > 0) {
            item {
                AnalyticsSummaryCard(
                    totalAmount = uiState.totalSpending,
                    transactionCount = uiState.transactionCount,
                    averageAmount = uiState.averageAmount,
                    topCategory = uiState.topCategory,
                    topCategoryPercentage = uiState.topCategoryPercentage,
                    isLoading = uiState.isLoading
                )
            }
        }
        
        // Category Breakdown Section
        if (uiState.categoryBreakdown.isNotEmpty()) {
            item {
                CategoryBreakdownCard(
                    categories = uiState.categoryBreakdown,
                    onCategoryClick = { category ->
                        onNavigateToTransactions(category.name, null, selectedPeriod.name)
                    }
                )
            }
        }
        
        // Top Merchants Section
        if (uiState.topMerchants.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Top Merchants"
                )
            }
            
            // All Merchants with expandable list
            item {
                ExpandableList(
                    items = uiState.topMerchants,
                    visibleItemCount = 3,
                    modifier = Modifier.fillMaxWidth()
                ) { merchant ->
                    MerchantListItem(
                        merchant = merchant,
                        onClick = {
                            onNavigateToTransactions(null, merchant.name, selectedPeriod.name)
                        }
                    )
                }
            }
        }
        
        
        // Empty state
        if (uiState.topMerchants.isEmpty() && uiState.categoryBreakdown.isEmpty() && !uiState.isLoading) {
            item {
                EmptyAnalyticsState()
            }
        }
    }
    
    // Chat FAB
    SmallFloatingActionButton(
        onClick = onNavigateToChat,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(Dimensions.Padding.content),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = "Open AI Assistant"
        )
    }
    }
}

@Composable
private fun CategoryListItem(
    category: CategoryData
) {
    val categoryInfo = CategoryMapping.categories[category.name]
        ?: CategoryMapping.categories["Others"]!!
    
    ListItemCard(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryInfo.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    category = category.name,
                    size = 24.dp,
                    tint = categoryInfo.color
                )
            }
        },
        title = category.name,
        subtitle = "${category.transactionCount} transactions",
        amount = CurrencyFormatter.formatCurrency(category.amount),
        trailingContent = {
            Text(
                text = "${category.percentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun MerchantListItem(
    merchant: MerchantData,
    onClick: () -> Unit = {}
) {
    val subtitle = buildString {
        append("${merchant.transactionCount} ")
        append(if (merchant.transactionCount == 1) "transaction" else "transactions")
        if (merchant.isSubscription) {
            append(" • Subscription")
        }
    }
    
    ListItemCard(
        leadingContent = {
            BrandIcon(
                merchantName = merchant.name,
                size = 40.dp,
                showBackground = true
            )
        },
        title = merchant.name,
        subtitle = subtitle,
        amount = CurrencyFormatter.formatCurrency(merchant.amount),
        onClick = onClick
    )
}

@Composable
private fun EmptyAnalyticsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.Padding.content),
        contentAlignment = Alignment.Center
    ) {
        PennyWiseCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.empty),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Start tracking expenses to see analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}