package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun AccountBalancesCard(
    accountBalances: List<AccountBalanceEntity>,
    totalBalance: BigDecimal,
    onViewAllClick: () -> Unit = {},
    onAccountClick: (bankName: String, accountLast4: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val displayBalances = if (accountBalances.size <= 5) {
        accountBalances
    } else {
        accountBalances.take(4)
    }
    
    PennyWiseCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.Padding.content)
        ) {
            // Total balance - prominent but minimal
            Text(
                text = CurrencyFormatter.formatCurrency(totalBalance),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${accountBalances.size} ${if (accountBalances.size == 1) "account" else "accounts"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (accountBalances.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                // Individual account balances - compact list
                displayBalances.forEach { balance ->
                    AccountBalanceItem(
                        balance = balance,
                        onClick = { onAccountClick(balance.bankName, balance.accountLast4) }
                    )
                }
                
                // View all link - only if more than 5 accounts
                if (accountBalances.size > 5) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "View all ${accountBalances.size} accounts →",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewAllClick() }
                            .padding(vertical = Spacing.xs)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountBalanceItem(
    balance: AccountBalanceEntity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = balance.bankName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "••${balance.accountLast4}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = CurrencyFormatter.formatCurrency(balance.balance),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}