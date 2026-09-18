package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import com.example.data.model.DpsScheme
import com.example.data.model.Person
import com.example.data.model.Project
import com.example.data.model.ProjectEntry
import com.example.ui.components.EditProjectEntryDialog
import com.example.ui.components.EditProjectNameDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryCyan
import com.example.ui.viewmodel.FundFlowUiState
import com.example.ui.viewmodel.FundFlowViewModel
import com.example.ui.viewmodel.LedgerSubTab
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    uiState: FundFlowUiState,
    viewModel: FundFlowViewModel,
    onOpenAddPerson: () -> Unit,
    onOpenSettlePerson: (Person) -> Unit,
    onOpenExtendPerson: (Person) -> Unit,
    onOpenAddDps: () -> Unit,
    onOpenPayDps: (DpsScheme) -> Unit,
    onOpenAddProject: () -> Unit,
    onOpenProjectEntry: (Project) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Sub-tabs bar
        PrimaryTabRow(
            selectedTabIndex = uiState.selectedLedgerSubTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            LedgerSubTab.values().forEach { tab ->
                val icon = when (tab) {
                    LedgerSubTab.KHATA -> Icons.Default.People
                    LedgerSubTab.DPS -> Icons.Default.Savings
                    LedgerSubTab.PROJECTS -> Icons.Default.Folder
                }
                Tab(
                    selected = uiState.selectedLedgerSubTab == tab,
                    onClick = { viewModel.setLedgerSubTab(tab) },
                    text = { Text(tab.title, fontWeight = FontWeight.Bold) },
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("subtab_${tab.name.lowercase()}")
                )
            }
        }

        when (uiState.selectedLedgerSubTab) {
            LedgerSubTab.KHATA -> KhataSubView(
                uiState = uiState,
                viewModel = viewModel,
                onAddPerson = onOpenAddPerson,
                onSettle = onOpenSettlePerson,
                onExtend = onOpenExtendPerson
            )
            LedgerSubTab.DPS -> DpsSubView(
                uiState = uiState,
                viewModel = viewModel,
                onAddDps = onOpenAddDps,
                onPayDps = onOpenPayDps
            )
            LedgerSubTab.PROJECTS -> ProjectsSubView(
                uiState = uiState,
                viewModel = viewModel,
                onAddProject = onOpenAddProject,
                onAddEntry = onOpenProjectEntry
            )
        }
    }
}

// ---------------- Khata SubView ----------------
@Composable
fun KhataSubView(
    uiState: FundFlowUiState,
    viewModel: FundFlowViewModel,
    onAddPerson: () -> Unit,
    onSettle: (Person) -> Unit,
    onExtend: (Person) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, receivable, payable, overdue

    val filteredPersons = remember(uiState.persons, searchQuery, filterType) {
        uiState.persons.filter { p ->
            val matchesFilter = when (filterType) {
                "receivable" -> p.type == "receivable" && !p.isSettled
                "payable" -> p.type == "payable" && !p.isSettled
                "overdue" -> DateUtils.isOverdue(p.dueDate) && !p.isSettled
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) true else {
                p.name.contains(searchQuery, ignoreCase = true) || p.phone.contains(searchQuery)
            }
            matchesFilter && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("মোট পাওনা", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            CurrencyFormatter.formatBDT(uiState.totalReceivable),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan
                        )
                    }
                }
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("মোট দেনা", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            CurrencyFormatter.formatBDT(uiState.totalPayable),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentRose
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ব্যক্তি হিসাব তালিকা", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onAddPerson,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.testTag("add_person_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ নতুন ব্যক্তি")
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("ব্যক্তি বা মোবাইল নম্বর খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("সব") }
                )
                FilterChip(
                    selected = filterType == "receivable",
                    onClick = { filterType = "receivable" },
                    label = { Text("আমি পাবো") }
                )
                FilterChip(
                    selected = filterType == "payable",
                    onClick = { filterType = "payable" },
                    label = { Text("সে পাবে") }
                )
                FilterChip(
                    selected = filterType == "overdue",
                    onClick = { filterType = "overdue" },
                    label = { Text("বকেয়া ⚠️") }
                )
            }
        }

        if (filteredPersons.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "খাতায় কোনো ব্যক্তি রেকর্ড পাওয়া যায়নি",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(filteredPersons, key = { it.id }) { person ->
            PersonCard(
                person = person,
                isEditMode = uiState.isEditMode,
                onSettle = { onSettle(person) },
                onExtend = { onExtend(person) },
                onDelete = { viewModel.deletePerson(person) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun PersonCard(
    person: Person,
    isEditMode: Boolean,
    onSettle: () -> Unit,
    onExtend: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isRec = person.type == "receivable"
    val isOverdue = !person.isSettled && DateUtils.isOverdue(person.dueDate)
    val color = if (person.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else (if (isRec) SecondaryCyan else AccentRose)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            person.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (person.isSettled) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AccentEmerald.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "পরিশোধিত",
                                    color = AccentEmerald,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (person.phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(person.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            CurrencyFormatter.formatBDT(person.balance),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = color
                        )
                        Text(
                            if (isRec) "পাওনা" else "দেনা",
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                    }

                    if (isEditMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Meta Info Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "শুরু: ${DateUtils.formatDisplay(person.txDate)} (${person.accountName})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = if (isOverdue) AccentRose.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            if (isOverdue) Icons.Default.HourglassTop else Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (isOverdue) AccentRose else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (person.isSettled) "সম্পন্ন" else "পরিশোধ: ${DateUtils.formatDisplay(person.dueDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) AccentRose else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Action Buttons
            if (!person.isSettled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSettle,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("settle_person_btn_${person.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পরিশোধ")
                    }

                    OutlinedButton(
                        onClick = onExtend,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("extend_person_btn_${person.id}")
                    ) {
                        Text("সময় বাড়ান")
                    }
                }
            }
        }
    }
}

// ---------------- DPS SubView ----------------
@Composable
fun DpsSubView(
    uiState: FundFlowUiState,
    viewModel: FundFlowViewModel,
    onAddDps: () -> Unit,
    onPayDps: (DpsScheme) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // DPS Header Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AccentIndigo.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("মোট সঞ্চিত ডিপিএস ফান্ড", style = MaterialTheme.typography.bodySmall, color = AccentIndigo)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            CurrencyFormatter.formatBDT(uiState.totalDpsAccumulated),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentIndigo
                        )
                    }
                    Button(
                        onClick = onAddDps,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_dps_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ নতুন ডিপিএস")
                    }
                }
            }
        }

        if (uiState.dpsSchemes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "কোনো সক্রিয় ডিপিএস স্কিম নেই। নতুন ডিপিএস যুক্ত করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(uiState.dpsSchemes, key = { it.id }) { dps ->
            DpsCard(
                dps = dps,
                isEditMode = uiState.isEditMode,
                onPay = { onPayDps(dps) },
                onDelete = { viewModel.deleteDps(dps) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DpsCard(
    dps: DpsScheme,
    isEditMode: Boolean,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentIndigo.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(dps.bankName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${dps.durationYears} বছর মেয়াদী", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        CurrencyFormatter.formatBDT(dps.totalDeposited),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentIndigo
                    )
                    if (isEditMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Stats Matrix
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("মাসিক কিস্তি", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.formatBDT(dps.monthlyAmount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("পরিশোধিত", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${dps.totalInstallmentsPaid} মাস", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("জমার দিন", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("প্রতি মাসের ${dps.dayOfMonth} তারিখ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "পরবর্তী কিস্তি: ${DateUtils.formatDisplay(dps.nextDueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = onPay,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("pay_dps_btn_${dps.id}")
                ) {
                    Text("কিস্তি দিন")
                }
            }
        }
    }
}

// ---------------- Projects SubView ----------------
@Composable
fun ProjectsSubView(
    uiState: FundFlowUiState,
    viewModel: FundFlowViewModel,
    onAddProject: () -> Unit,
    onAddEntry: (Project) -> Unit
) {
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var editingEntry by remember { mutableStateOf<ProjectEntry?>(null) }

    if (editingProject != null) {
        EditProjectNameDialog(
            project = editingProject!!,
            onDismiss = { editingProject = null },
            onConfirm = { newName ->
                viewModel.editProjectName(editingProject!!.id, newName)
                editingProject = null
            }
        )
    }

    if (editingEntry != null) {
        EditProjectEntryDialog(
            entry = editingEntry!!,
            accounts = uiState.accounts,
            onDismiss = { editingEntry = null },
            onConfirm = { updated ->
                viewModel.editProjectEntry(updated)
                editingEntry = null
            },
            onDelete = {
                viewModel.deleteProjectEntry(editingEntry!!)
                editingEntry = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("সক্রিয় প্রজেক্ট তালিকা (${uiState.projects.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onAddProject,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.testTag("add_project_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ নতুন প্রজেক্ট")
                }
            }
        }

        if (uiState.projects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "কোনো প্রজেক্ট নেই। বাড়ি নির্মাণ, ইভেন্ট বা ব্যবসা প্রজেক্ট তৈরি করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(uiState.projects, key = { it.id }) { project ->
            val summary = uiState.projectBalances[project.id]
            val entries = uiState.projectEntries.filter { it.projectId == project.id }
            ProjectCard(
                project = project,
                income = summary?.income ?: 0.0,
                expense = summary?.expense ?: 0.0,
                net = summary?.net ?: 0.0,
                entries = entries,
                isEditMode = uiState.isEditMode,
                onAddEntry = { onAddEntry(project) },
                onEditProject = { editingProject = project },
                onEditEntry = { editingEntry = it },
                onDelete = { viewModel.deleteProject(project) },
                onDeleteEntry = { viewModel.deleteProjectEntry(it) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    income: Double,
    expense: Double,
    net: Double,
    entries: List<ProjectEntry>,
    isEditMode: Boolean,
    onAddEntry: () -> Unit,
    onEditProject: () -> Unit,
    onEditEntry: (ProjectEntry) -> Unit,
    onDelete: () -> Unit,
    onDeleteEntry: (ProjectEntry) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEditProject() }
                ) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onAddEntry,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        modifier = Modifier.testTag("project_entry_btn_${project.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("এন্ট্রি")
                    }
                    if (isEditMode) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Stats Matrix
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("মোট আয়", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.formatBDT(income), fontWeight = FontWeight.Bold, color = AccentEmerald, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("মোট ব্যয়", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.formatBDT(expense), fontWeight = FontWeight.Bold, color = AccentRose, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("নিট স্থিতি", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.formatBDT(net), fontWeight = FontWeight.ExtraBold, color = if (net >= 0) AccentEmerald else AccentRose, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Expand Entries Toggle
            if (entries.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${entries.size} টি প্রজেক্ট লেনদেন বিবরণ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                }

                if (expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        entries.forEach { entry ->
                            val isInc = entry.type == "income"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .clickable { onEditEntry(entry) }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text("${DateUtils.formatDisplay(entry.date)} • ${entry.accountName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${if (isInc) "+" else "-"} ${CurrencyFormatter.formatBDT(entry.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isInc) AccentEmerald else AccentRose
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(onClick = { onEditEntry(entry) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Entry", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    if (isEditMode) {
                                        Spacer(modifier = Modifier.width(2.dp))
                                        IconButton(onClick = { onDeleteEntry(entry) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
