package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.CategoryChip
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val editableTransaction by viewModel.editableTransaction.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val applyToAllFromMerchant by viewModel.applyToAllFromMerchant.collectAsStateWithLifecycle()
    val updateExistingTransactions by viewModel.updateExistingTransactions.collectAsStateWithLifecycle()
    val existingTransactionCount by viewModel.existingTransactionCount.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Show success snackbar
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar("Transaction updated successfully")
                viewModel.clearSaveSuccess()
            }
        }
    }
    
    // Show error snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }
    
    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }
    
    val context = LocalContext.current
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            // Show Report Issue FAB only when not in edit mode and transaction exists
            if (!isEditMode && transaction != null) {
                FloatingActionButton(
                    onClick = {
                        val reportUrl = viewModel.getReportUrl()
                        android.util.Log.d("TransactionDetail", "Report FAB clicked, opening URL: ${reportUrl.take(200)}...")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reportUrl))
                        try {
                            context.startActivity(intent)
                            android.util.Log.d("TransactionDetail", "Successfully launched browser intent")
                        } catch (e: Exception) {
                            android.util.Log.e("TransactionDetail", "Error launching browser", e)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Report Issue"
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Transaction" else "Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) {
                            viewModel.cancelEdit()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (isEditMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEditMode) "Cancel" else "Back"
                        )
                    }
                },
                actions = {
                    if (!isEditMode && transaction != null) {
                        IconButton(onClick = { viewModel.enterEditMode() }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit"
                            )
                        }
                    } else if (isEditMode) {
                        TextButton(
                            onClick = { viewModel.saveChanges() },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimensions.Icon.small),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Save")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val displayTransaction = if (isEditMode) editableTransaction else transaction
        displayTransaction?.let { txn ->
            TransactionDetailContent(
                transaction = txn,
                isEditMode = isEditMode,
                applyToAllFromMerchant = applyToAllFromMerchant,
                updateExistingTransactions = updateExistingTransactions,
                existingTransactionCount = existingTransactionCount,
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun TransactionDetailContent(
    transaction: TransactionEntity,
    isEditMode: Boolean,
    applyToAllFromMerchant: Boolean,
    updateExistingTransactions: Boolean,
    existingTransactionCount: Int,
    viewModel: TransactionDetailViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()  // Add IME padding to push content up when keyboard appears
            .verticalScroll(scrollState)
            .padding(Dimensions.Padding.content)
    ) {
        // Header with amount and merchant
        if (isEditMode) {
            EditableTransactionHeader(
                transaction = transaction,
                viewModel = viewModel
            )
        } else {
            TransactionHeader(transaction)
        }
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // SMS Body - Always read-only
        if (!transaction.smsBody.isNullOrBlank()) {
            SmsBodyCard(transaction.smsBody)
            Spacer(modifier = Modifier.height(Spacing.md))
        }
        
        // Extracted Information
        if (isEditMode) {
            EditableExtractedInfoCard(
                transaction = transaction,
                applyToAllFromMerchant = applyToAllFromMerchant,
                updateExistingTransactions = updateExistingTransactions,
                existingTransactionCount = existingTransactionCount,
                viewModel = viewModel
            )
        } else {
            ExtractedInfoCard(transaction)
        }
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        // Additional Details - Always read-only
        if (transaction.balanceAfter != null || transaction.accountNumber != null) {
            AdditionalDetailsCard(transaction)
        }
        
        // Add extra bottom padding when in edit mode to ensure description field is visible above keyboard
        if (isEditMode) {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun TransactionHeader(transaction: TransactionEntity) {
    PennyWiseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Icon
            BrandIcon(
                merchantName = transaction.merchantName,
                size = 64.dp,
                showBackground = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Merchant Name
            Text(
                text = transaction.merchantName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Amount with sign
            val amountColor = when (transaction.transactionType) {
                TransactionType.INCOME -> Color(0xFF4CAF50)
                TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                TransactionType.CREDIT -> Color(0xFFFF6B35)  // Orange for credit
                TransactionType.TRANSFER -> Color(0xFF9C27B0)  // Purple for transfer
                TransactionType.INVESTMENT -> Color(0xFF00796B)  // Teal for investment
            }
            val sign = when (transaction.transactionType) {
                TransactionType.INCOME -> "+"
                TransactionType.EXPENSE -> "-"
                TransactionType.CREDIT -> "💳"
                TransactionType.TRANSFER -> "↔"
                TransactionType.INVESTMENT -> "📈"
            }
            
            Text(
                text = "$sign${CurrencyFormatter.formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            
            // Date and Time
            Text(
                text = transaction.dateTime.format(
                    DateTimeFormatter.ofPattern("MMMM d, yyyy • h:mm a")
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmsBodyCard(smsBody: String) {
    PennyWiseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Original SMS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // SMS text in monospace font
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = smsBody,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(Spacing.md)
                )
            }
        }
    }
}

@Composable
private fun ExtractedInfoCard(transaction: TransactionEntity) {
    PennyWiseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Extracted Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Category
            InfoRow(
                label = "Category",
                value = transaction.category,
                icon = Icons.Default.Category
            )
            
            // Bank
            transaction.bankName?.let {
                InfoRow(
                    label = "Bank",
                    value = it,
                    icon = Icons.Default.AccountBalance
                )
            }
            
            // Transaction Type
            InfoRow(
                label = "Type",
                value = transaction.transactionType.name.lowercase().replaceFirstChar { it.uppercase() },
                icon = when (transaction.transactionType) {
                    TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
                    TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
                    TransactionType.CREDIT -> Icons.Default.CreditCard
                    TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                    TransactionType.INVESTMENT -> Icons.Default.ShowChart
                }
            )
            
            // Description
            transaction.description?.let {
                InfoRow(
                    label = "Description",
                    value = it,
                    icon = Icons.Default.Description
                )
            }
            
            // Recurring status
            if (transaction.isRecurring) {
                InfoRow(
                    label = "Status",
                    value = "Recurring Transaction",
                    icon = Icons.Default.Repeat
                )
            }
        }
    }
}

@Composable
private fun AdditionalDetailsCard(transaction: TransactionEntity) {
    PennyWiseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Additional Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Account Number (masked)
            transaction.accountNumber?.let {
                val masked = if (it.length > 4) {
                    "*".repeat(it.length - 4) + it.takeLast(4)
                } else it
                InfoRow(
                    label = "Account",
                    value = masked,
                    icon = Icons.Default.CreditCard
                )
            }
            
            // Balance After
            transaction.balanceAfter?.let {
                InfoRow(
                    label = "Balance After",
                    value = CurrencyFormatter.formatCurrency(it),
                    icon = Icons.Default.AccountBalanceWallet
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimensions.Icon.small)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableTransactionHeader(
    transaction: TransactionEntity,
    viewModel: TransactionDetailViewModel
) {
    PennyWiseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Merchant Name
            OutlinedTextField(
                value = transaction.merchantName,
                onValueChange = { viewModel.updateMerchantName(it) },
                label = { Text("Merchant Name") },
                leadingIcon = {
                    BrandIcon(
                        merchantName = transaction.merchantName,
                        size = 24.dp,
                        showBackground = false
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = transaction.merchantName.isBlank()
            )
            
            // Amount
            OutlinedTextField(
                value = transaction.amount.toPlainString(),
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Amount") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Transaction Type - All in one row with wrapping
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                FilterChip(
                    selected = transaction.transactionType == TransactionType.INCOME,
                    onClick = { viewModel.updateTransactionType(TransactionType.INCOME) },
                    label = { 
                        Text(
                            text = "Income",
                            maxLines = 1
                        ) 
                    },
                    leadingIcon = if (transaction.transactionType == TransactionType.INCOME) {
                        { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = transaction.transactionType == TransactionType.EXPENSE,
                    onClick = { viewModel.updateTransactionType(TransactionType.EXPENSE) },
                    label = { 
                        Text(
                            text = "Expense",
                            maxLines = 1
                        ) 
                    },
                    leadingIcon = if (transaction.transactionType == TransactionType.EXPENSE) {
                        { Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = transaction.transactionType == TransactionType.CREDIT,
                    onClick = { viewModel.updateTransactionType(TransactionType.CREDIT) },
                    label = { 
                        Text(
                            text = "Credit",
                            maxLines = 1
                        ) 
                    },
                    leadingIcon = if (transaction.transactionType == TransactionType.CREDIT) {
                        { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Transaction Type - Second Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                FilterChip(
                    selected = transaction.transactionType == TransactionType.TRANSFER,
                    onClick = { viewModel.updateTransactionType(TransactionType.TRANSFER) },
                    label = { 
                        Text(
                            text = "Transfer",
                            maxLines = 1
                        ) 
                    },
                    leadingIcon = if (transaction.transactionType == TransactionType.TRANSFER) {
                        { Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = transaction.transactionType == TransactionType.INVESTMENT,
                    onClick = { viewModel.updateTransactionType(TransactionType.INVESTMENT) },
                    label = { 
                        Text(
                            text = "Investment",
                            maxLines = 1
                        ) 
                    },
                    leadingIcon = if (transaction.transactionType == TransactionType.INVESTMENT) {
                        { Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Date and Time
            DateTimeField(
                dateTime = transaction.dateTime,
                onDateTimeChange = { viewModel.updateDateTime(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableExtractedInfoCard(
    transaction: TransactionEntity,
    applyToAllFromMerchant: Boolean,
    updateExistingTransactions: Boolean,
    existingTransactionCount: Int,
    viewModel: TransactionDetailViewModel
) {
    PennyWiseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Transaction Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Category Dropdown
            CategoryDropdown(
                selectedCategory = transaction.category,
                onCategorySelected = { viewModel.updateCategory(it) },
                viewModel = viewModel
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Apply to all from merchant checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = applyToAllFromMerchant,
                    onCheckedChange = { viewModel.toggleApplyToAllFromMerchant() }
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Apply this category to all future transactions from ${transaction.merchantName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Update existing transactions checkbox (only show if there are other transactions)
            if (existingTransactionCount > 0) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = updateExistingTransactions,
                        onCheckedChange = { viewModel.toggleUpdateExistingTransactions() }
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "Also update $existingTransactionCount existing ${if (existingTransactionCount == 1) "transaction" else "transactions"} from ${transaction.merchantName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Description
            OutlinedTextField(
                value = transaction.description ?: "",
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description (Optional)") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Recurring checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = transaction.isRecurring,
                    onCheckedChange = { viewModel.updateRecurringStatus(it) }
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Recurring Transaction",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // Bank (read-only)
            transaction.bankName?.let {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimensions.Icon.small)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "Bank: $it (cannot be edited)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    viewModel: TransactionDetailViewModel
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    var expanded by remember { mutableStateOf(false) }
    
    // Find the selected category entity for displaying with color
    val selectedCategoryEntity = categories.find { it.name == selectedCategory }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = { },
            label = { Text("Category") },
            leadingIcon = {
                if (selectedCategoryEntity != null) {
                    CategoryChip(
                        category = selectedCategoryEntity,
                        showText = false,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            readOnly = true
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { 
                        CategoryChip(category = category)
                    },
                    onClick = {
                        onCategorySelected(category.name)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeField(
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Date Field
        OutlinedTextField(
            value = dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
            onValueChange = { },
            label = { Text("Date") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, "Select date")
                }
            },
            modifier = Modifier.weight(1f)
        )
        
        // Time Field
        OutlinedTextField(
            value = dateTime.format(DateTimeFormatter.ofPattern("h:mm a")),
            onValueChange = { },
            label = { Text("Time") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showTimePicker = true }) {
                    Icon(Icons.Default.Schedule, "Select time")
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.toLocalDate().toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onDateTimeChange(dateTime.withYear(newDate.year)
                            .withMonth(newDate.monthValue)
                            .withDayOfMonth(newDate.dayOfMonth))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    onDateTimeChange(dateTime.withHour(timePickerState.hour)
                        .withMinute(timePickerState.minute))
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}