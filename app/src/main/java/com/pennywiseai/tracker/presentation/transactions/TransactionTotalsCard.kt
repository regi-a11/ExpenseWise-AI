package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun TransactionTotalsCard(
    income: BigDecimal,
    expenses: BigDecimal,
    netBalance: BigDecimal,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val incomeAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.5f else 1f,
        animationSpec = tween(300),
        label = "income_alpha"
    )
    
    val expenseAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.5f else 1f,
        animationSpec = tween(300),
        label = "expense_alpha"
    )
    
    val netAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.5f else 1f,
        animationSpec = tween(300),
        label = "net_alpha"
    )
    
    PennyWiseCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Income Column
            TotalColumn(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Income",
                        modifier = Modifier.size(20.dp),
                        tint = if (!isSystemInDarkTheme()) income_light else income_dark
                    )
                },
                label = "Income",
                amount = CurrencyFormatter.formatCurrency(income),
                color = if (!isSystemInDarkTheme()) income_light else income_dark,
                modifier = Modifier
                    .weight(1f)
                    .alpha(incomeAlpha)
            )
            
            // Vertical Divider
            VerticalDivider(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = Spacing.xs),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            
            // Expenses Column
            TotalColumn(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = "Expenses",
                        modifier = Modifier.size(20.dp),
                        tint = if (!isSystemInDarkTheme()) expense_light else expense_dark
                    )
                },
                label = "Expenses",
                amount = CurrencyFormatter.formatCurrency(expenses),
                color = if (!isSystemInDarkTheme()) expense_light else expense_dark,
                modifier = Modifier
                    .weight(1f)
                    .alpha(expenseAlpha)
            )
            
            // Vertical Divider
            VerticalDivider(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = Spacing.xs),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            
            // Net Balance Column
            val netColor = when {
                netBalance > BigDecimal.ZERO -> if (!isSystemInDarkTheme()) income_light else income_dark
                netBalance < BigDecimal.ZERO -> if (!isSystemInDarkTheme()) expense_light else expense_dark
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            val netPrefix = when {
                netBalance > BigDecimal.ZERO -> "+"
                else -> ""
            }
            
            TotalColumn(
                icon = null,
                label = "Net",
                amount = "$netPrefix${CurrencyFormatter.formatCurrency(netBalance)}",
                color = netColor,
                modifier = Modifier
                    .weight(1f)
                    .alpha(netAlpha)
            )
        }
    }
}

@Composable
private fun TotalColumn(
    icon: @Composable (() -> Unit)?,
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}