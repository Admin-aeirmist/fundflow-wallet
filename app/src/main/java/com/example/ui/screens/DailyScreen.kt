package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.components.AccountDropdownField
import com.example.ui.components.EditTransactionDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.viewmodel.FundFlowUiState
import com.example.ui.viewmodel.FundFlowViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun DailyScreen(
    uiState: FundFlowUiState,
    viewModel: FundFlowViewModel,
    modifier: Modifier = Modifier
) {
    // Form state
    var selectedType by remember { mutableStateOf("expense") } // expense, income, pending
    var selectedAccount by remember { mutableStateOf(uiState.accounts.firstOrNull()?.name ?: "ক্যাশ") }
    var amountText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(DateUtils.today()) }
    var descText by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    // Search & Filter state
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, expense, income, pending
    var editingTx by remember { mutableStateOf<Transaction?>(null) }

    if (editingTx != null) {
        EditTransactionDialog(
            transaction = editingTx!!,
            accounts = uiState.accounts,
            onDismiss = { editingTx = null },
            onConfirm = { updated ->
                viewModel.updateTransaction(updated)
                editingTx = null
            },
            onDelete = {
                viewModel.deleteTransaction(editingTx!!)
                editingTx = null
            }
        )
    }

    val filteredList = remember(uiState.transactions, filterType, searchQuery) {
        uiState.transactions.filter { tx ->
            val matchFilter = when (filterType) {
                "ALL" -> true
                else -> tx.type.equals(filterType, ignoreCase = true)
            }
            val matchQuery = if (searchQuery.isBlank()) true else {
                tx.description.contains(searchQuery, ignoreCase = true) ||
                        tx.accountName.contains(searchQuery, ignoreCase = true)
            }
            matchFilter && matchQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Quick Add Form Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "নতুন আয় / ব্যয় / পেন্ডিং এন্ট্রি",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Type Selector Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeChip(
                            label = "খরচ",
                            selected = selectedType == "expense",
                            color = AccentRose,
                            onClick = { selectedType = "expense" },
                            modifier = Modifier.weight(1f)
                        )
                        TypeChip(
                            label = "আয়",
                            selected = selectedType == "income",
                            color = AccentEmerald,
                            onClick = { selectedType = "income" },
                            modifier = Modifier.weight(1f)
                        )
                        TypeChip(
                            label = "পেন্ডিং",
                            selected = selectedType == "pending",
                            color = AccentAmber,
                            onClick = { selectedType = "pending" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AccountDropdownField(
                        label = "পেমেন্ট মাধ্যম",
                        accounts = uiState.accounts,
                        selectedAccount = selectedAccount,
                        onAccountSelected = { selectedAccount = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it; formError = null },
                            label = { Text("পরিমাণ (৳) *") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("daily_amount_input")
                        )
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = { dateText = it },
                            label = { Text("তারিখ") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("daily_date_input")
                        )
                    }

                    OutlinedTextField(
                        value = descText,
                        onValueChange = { descText = it; formError = null },
                        label = { Text("বিবরণ *") },
                        placeholder = { Text("যেমন: বাজার খরচ, বেতন, মোবাইল রিচার্জ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_desc_input")
                    )

                    if (formError != null) {
                        Text(
                            formError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount <= 0) {
                                formError = "সঠিক টাকার পরিমাণ দিন"
                                return@Button
                            }
                            if (descText.isBlank()) {
                                formError = "বিবরণ আবশ্যক"
                                return@Button
                            }
                            viewModel.addTransaction(
                                type = selectedType,
                                account = selectedAccount,
                                amount = amount,
                                date = dateText,
                                desc = descText
                            )
                            // Reset form fields
                            amountText = ""
                            descText = ""
                            dateText = DateUtils.today()
                            formError = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("daily_submit_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ এন্ট্রি যোগ করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search and Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("লেনদেন খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = filterType == "ALL",
                            onClick = { filterType = "ALL" },
                            label = { Text("সব (${uiState.transactions.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == "expense",
                            onClick = { filterType = "expense" },
                            label = { Text("খরচ") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == "income",
                            onClick = { filterType = "income" },
                            label = { Text("আয়") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == "pending",
                            onClick = { filterType = "pending" },
                            label = { Text("পেন্ডিং") }
                        )
                    }
                }
            }
        }

        // Transactions List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "লেনদেন তালিকা (${filteredList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.isEditMode) {
                    Text(
                        "মুছতে ✕ বাটন চাপুন",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "কোনো লেনদেন পাওয়া যায়নি",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(filteredList, key = { it.id }) { tx ->
            TransactionItemCard(
                transaction = tx,
                isEditMode = uiState.isEditMode,
                onEdit = { editingTx = tx },
                onDelete = { viewModel.deleteTransaction(tx) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun TypeChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    isEditMode: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isInc = transaction.type == "income"
    val isPend = transaction.type == "pending"
    val icon = if (isInc) Icons.Default.NorthEast else (if (isPend) Icons.Default.HourglassEmpty else Icons.Default.SouthWest)
    val color = if (isInc) AccentEmerald else (if (isPend) AccentAmber else AccentRose)
    val prefix = if (isInc) "+" else (if (isPend) "⌛" else "-")

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            DateUtils.formatDisplay(transaction.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                transaction.accountName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$prefix ${CurrencyFormatter.formatBDT(transaction.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    style = MaterialTheme.typography.titleSmall
                )
                if (isEditMode) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
