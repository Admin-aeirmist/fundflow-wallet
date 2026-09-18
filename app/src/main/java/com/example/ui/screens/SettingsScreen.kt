package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CloudSyncDialog
import com.example.ui.components.DateRangeReportDialog
import com.example.ui.components.GoogleSheetsSyncDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryCyan
import com.example.ui.viewmodel.FundFlowUiState
import com.example.ui.viewmodel.FundFlowViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun SettingsScreen(
    uiState: FundFlowUiState,
    viewModel: FundFlowViewModel,
    onOpenAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showGoogleSheetsDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showDateRangeDialog by remember { mutableStateOf(false) }

    val totalIncome = remember(uiState.transactions) {
        uiState.transactions.filter { it.type == "income" }.sumOf { it.amount }
    }
    val totalExpense = remember(uiState.transactions) {
        uiState.transactions.filter { it.type == "expense" }.sumOf { it.amount }
    }

    val summaryReportText = remember(uiState, totalIncome, totalExpense) {
        """
        📊 FundFlow - আর্থিক বিবরণী
        তারিখ: ${DateUtils.today()}
        ----------------------------------
        💵 তরল ক্যাশ স্থিতি: ${CurrencyFormatter.formatBDT(uiState.totalNetLiquidCash)}
        📈 মোট আয়: ${CurrencyFormatter.formatBDT(totalIncome)}
        📉 মোট ব্যয়: ${CurrencyFormatter.formatBDT(totalExpense)}
        🏗️ প্রজেক্ট নিট লাভ: ${CurrencyFormatter.formatBDT(uiState.totalProjectsNetProfit)}
        
        👥 খাতা মোট পাওনা: ${CurrencyFormatter.formatBDT(uiState.totalReceivable)}
        👥 খাতা মোট দেনা: ${CurrencyFormatter.formatBDT(uiState.totalPayable)}
        
        🏦 সঞ্চিত ডিপিএস ফান্ড: ${CurrencyFormatter.formatBDT(uiState.totalDpsAccumulated)}
        ⌛ পেন্ডিং হিসাব: ${CurrencyFormatter.formatBDT(uiState.totalPendingBills)}
        ----------------------------------
        FundFlow দ্বারা প্রস্তুতকৃত।
        """.trimIndent()
    }

    if (showGoogleSheetsDialog) {
        GoogleSheetsSyncDialog(
            currentUrl = uiState.googleSheetsUrl,
            isSyncing = uiState.isSyncing,
            onDismiss = { showGoogleSheetsDialog = false },
            onSaveUrl = { viewModel.saveGoogleSheetsUrl(it) },
            onSyncNow = { url -> viewModel.syncAllToGoogleSheets(url) }
        )
    }

    if (showCloudSyncDialog) {
        CloudSyncDialog(
            currentPin = uiState.cloudPin,
            isSyncing = uiState.isSyncing,
            onDismiss = { showCloudSyncDialog = false },
            onSavePin = { viewModel.saveCloudPin(it) },
            onPush = { pin -> viewModel.backupToCloud(pin) },
            onPull = { pin -> viewModel.restoreFromCloud(pin) },
            onExportJson = {
                viewModel.exportBackupJson { json ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("FundFlow Backup JSON", json))
                    Toast.makeText(context, "ব্যাকআপ JSON ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_LONG).show()
                }
            },
            onImportJson = { json ->
                viewModel.importBackupJson(json)
                showCloudSyncDialog = false
            }
        )
    }

    if (showDateRangeDialog) {
        DateRangeReportDialog(
            transactions = uiState.transactions,
            projectEntries = uiState.projectEntries,
            onDismiss = { showDateRangeDialog = false },
            onShareReport = { report ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "FundFlow আর্থিক বিবরণী রিপোর্ট")
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                context.startActivity(Intent.createChooser(shareIntent, "রিপোর্ট শেয়ার করুন"))
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Comprehensive Financial Statement Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "📊 আর্থিক স্থিতিপত্র (Financial Statement)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()

                    SummaryRow(label = "তরল ক্যাশ (বর্তমান ব্যালেন্স)", value = CurrencyFormatter.formatBDT(uiState.totalNetLiquidCash), color = PrimaryBlue, isBold = true)
                    SummaryRow(label = "সর্বমোট আয় (Inflow)", value = CurrencyFormatter.formatBDT(totalIncome), color = AccentEmerald)
                    SummaryRow(label = "সর্বমোট খরচ (Outflow)", value = CurrencyFormatter.formatBDT(totalExpense), color = AccentRose)
                    SummaryRow(label = "খাতায় মানুষের কাছে পাওনা", value = CurrencyFormatter.formatBDT(uiState.totalReceivable), color = SecondaryCyan)
                    SummaryRow(label = "খাতায় মানুষের কাছে দেনা", value = CurrencyFormatter.formatBDT(uiState.totalPayable), color = AccentRose)
                    SummaryRow(label = "ডিপিএস জমার মোট পরিমাণ", value = CurrencyFormatter.formatBDT(uiState.totalDpsAccumulated), color = AccentIndigo)
                    SummaryRow(label = "পেন্ডিং বিল বা দায়", value = CurrencyFormatter.formatBDT(uiState.totalPendingBills), color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Account Channels Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "পেমেন্ট মাধ্যম ব্যবস্থাপনা",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onOpenAddAccount,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.testTag("settings_add_account_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নতুন মাধ্যম")
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.accounts.forEach { acc ->
                        val bal = uiState.accountBalances[acc.name] ?: 0.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(acc.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                Text(if (acc.isDefault) "ডিফল্ট চ্যানেল" else "কাস্টম চ্যানেল", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(CurrencyFormatter.formatBDT(bal), fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                if (!acc.isDefault) {
                                    IconButton(onClick = { viewModel.deleteAccount(acc) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Share and Report Actions
        item {
            Text(
                "শেয়ার ও রিপোর্ট",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("FundFlow Summary", summaryReportText))
                        Toast.makeText(context, "আর্থিক বিবরণী ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).testTag("copy_summary_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("রিপোর্ট কপি")
                }

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "FundFlow আর্থিক বিবরণী")
                            putExtra(Intent.EXTRA_TEXT, summaryReportText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "বিবরণী শেয়ার করুন"))
                    },
                    modifier = Modifier.weight(1f).testTag("share_summary_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("রিপোর্ট শেয়ার")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showDateRangeDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("date_range_report_btn")
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("তারিখ অনুযায়ী হিসাব রিপোর্ট ও ফিল্টার")
            }
        }

        // Cloud & Google Sheets Sync Section
        item {
            Text(
                "ক্লাউড ব্যাকআপ ও গুগল শিট সিঙ্ক",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Google Sheets row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Google Sheets সিঙ্ক", fontWeight = FontWeight.Bold)
                                Text("আপনার সব হিসাব স্প্রেডশিটে ব্যাকআপ রাখুন", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.syncAllToGoogleSheets() },
                            modifier = Modifier.weight(1f).testTag("quick_sync_sheets_btn")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("শিটে পাঠান")
                        }
                        OutlinedButton(
                            onClick = { showGoogleSheetsDialog = true },
                            modifier = Modifier.weight(1f).testTag("setup_sheets_btn")
                        ) {
                            Text("সেটিংস")
                        }
                    }

                    HorizontalDivider()

                    // Cloud PIN row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ক্লাউড ব্যাকআপ ও পিন", fontWeight = FontWeight.Bold)
                                Text("পিন: ${if (uiState.cloudPin.isNotBlank()) uiState.cloudPin else "সেট করা হয়নি"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showCloudSyncDialog = true },
                            modifier = Modifier.weight(1f).testTag("cloud_sync_manage_btn")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ক্লাউড ব্যাকআপ / রিস্টোর")
                        }
                    }
                }
            }
        }

        // Danger Zone
        item {
            Text(
                "বিপজ্জনক জোন",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().testTag("reset_data_btn")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("সব ডেটা রিসেট করুন (Factory Reset)")
            }
        }

        // About
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("FundFlow (ফান্ডফ্লো) v2.0", fontWeight = FontWeight.Bold)
                        Text("নিরাপদ অফলাইন পার্সোনাল ও বিজনেস লেজার ও ফাইন্যান্স ট্র্যাকার", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("⚠️ ডেটা রিসেট নিশ্চিতকরণ", fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি নিশ্চিত যে সব লেনদেন, খাতা, ডিপিএস এবং প্রজেক্ট মুছে ফেলতে চান? এই পরিবর্তনটি পূর্বাবস্থায় ফিরিয়ে আনা যাবে না।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("হ্যাঁ, রিসেট করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    color: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}
