package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.Transaction
import com.example.ui.components.AccountDropdownField
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentEmeraldLight
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentIndigoLight
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentRoseLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryCyan
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.FundFlowUiState
import com.example.ui.viewmodel.FundFlowViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: FundFlowUiState,
    viewModel: FundFlowViewModel,
    onOpenTransfer: () -> Unit,
    onOpenAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var settlingTx by remember { mutableStateOf<Transaction?>(null) }
    var settleAccount by remember { mutableStateOf(uiState.accounts.firstOrNull()?.name ?: "ক্যাশ") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Hero Balance Card
            HeroBalanceCard(
                totalBalance = uiState.totalNetLiquidCash,
                onTransferClick = onOpenTransfer,
                onAddEntryClick = { viewModel.setTab(AppTab.DAILY) }
            )
        }

        item {
            // 4 Metrics Grid
            Text(
                "ফাইন্যান্সিয়াল মেট্রিক্স",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                MetricCard(
                    title = "পেন্ডিং বিল",
                    amount = uiState.totalPendingBills,
                    icon = Icons.Default.PendingActions,
                    accentColor = AccentAmber,
                    bgColor = AccentAmberLight,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "ডিপিএস সঞ্চয়",
                    amount = uiState.totalDpsAccumulated,
                    icon = Icons.Default.Savings,
                    accentColor = AccentIndigo,
                    bgColor = AccentIndigoLight,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "খাতা পাওনা",
                    amount = uiState.totalReceivable,
                    icon = Icons.Default.TrendingUp,
                    accentColor = SecondaryCyan,
                    bgColor = Color(0xFFE0F2FE),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "খাতা দেনা",
                    amount = uiState.totalPayable,
                    icon = Icons.Default.TrendingDown,
                    accentColor = AccentRose,
                    bgColor = AccentRoseLight,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "প্রজেক্ট নিট লাভ",
                    amount = uiState.totalProjectsNetProfit,
                    icon = Icons.Default.CreditCard,
                    accentColor = if (uiState.totalProjectsNetProfit >= 0) AccentEmerald else AccentRose,
                    bgColor = if (uiState.totalProjectsNetProfit >= 0) AccentEmeraldLight else AccentRoseLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // Dedicated Accounts Balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "পেমেন্ট মাধ্যম ও স্থিতি",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = onOpenAddAccount,
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.testTag("home_add_account_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নতুন মাধ্যম", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.accounts.forEach { acc ->
                    val bal = uiState.accountBalances[acc.name] ?: 0.0
                    AccountBalanceCard(
                        account = acc,
                        balance = bal,
                        isEditMode = uiState.isEditMode,
                        onDelete = { viewModel.deleteAccount(acc) }
                    )
                }
            }
        }

        // Pending Bills Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "পেন্ডিং হিসাব (${uiState.pendingTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.pendingTransactions.isNotEmpty()) {
                    Text(
                        "মোট: ${CurrencyFormatter.formatBDT(uiState.totalPendingBills)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentAmber
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.pendingTransactions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "আপাতত কোনো পেন্ডিং বিল বা বকেয়া নেই",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(uiState.pendingTransactions) { pendingTx ->
            PendingTxItemCard(
                transaction = pendingTx,
                onSettle = { settlingTx = pendingTx },
                isEditMode = uiState.isEditMode,
                onDelete = { viewModel.deleteTransaction(pendingTx) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Pending Quick Settle Dialog
    if (settlingTx != null) {
        val tx = settlingTx!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { settlingTx = null },
            title = { Text("পেন্ডিং বিল পরিশোধ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${tx.description} - ${CurrencyFormatter.formatBDT(tx.amount)}")
                    AccountDropdownField(
                        label = "যে মাধ্যম থেকে টাকা পরিশোধ করলেন",
                        accounts = uiState.accounts,
                        selectedAccount = settleAccount,
                        onAccountSelected = { settleAccount = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.settlePending(tx, settleAccount)
                        settlingTx = null
                    }
                ) {
                    Text("পরিশোধ নিশ্চিত করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { settlingTx = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun HeroBalanceCard(
    totalBalance: Double,
    onTransferClick: () -> Unit,
    onAddEntryClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "মোট তরল ক্যাশ (সব মাধ্যম মিলিয়ে)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "লাইভ স্থিতি",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                CurrencyFormatter.formatBDT(totalBalance),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.testTag("total_liquid_balance_text")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTransferClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hero_transfer_button")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ট্রান্সফার")
                }

                OutlinedButton(
                    onClick = onAddEntryClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hero_add_entry_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ এন্ট্রি")
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                CurrencyFormatter.formatBDT(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AccountBalanceCard(
    account: Account,
    balance: Double,
    isEditMode: Boolean,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        account.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (account.isDefault) "ডিফল্ট চ্যানেল" else "কাস্টম চ্যানেল",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    CurrencyFormatter.formatBDT(balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                if (isEditMode && !account.isDefault) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun PendingTxItemCard(
    transaction: Transaction,
    onSettle: () -> Unit,
    isEditMode: Boolean,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${DateUtils.formatDisplay(transaction.date)} • মাধ্যম: ${transaction.accountName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "⌛ ${CurrencyFormatter.formatBDT(transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = AccentAmber,
                    style = MaterialTheme.typography.titleSmall
                )
                Button(
                    onClick = onSettle,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.testTag("pending_settle_btn_${transaction.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("শোধ", style = MaterialTheme.typography.labelMedium)
                }
                if (isEditMode) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
