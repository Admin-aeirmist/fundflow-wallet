package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DpsScheme
import com.example.data.model.Person
import com.example.data.model.Project
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.AddDpsDialog
import com.example.ui.components.AddPersonDialog
import com.example.ui.components.AddProjectDialog
import com.example.ui.components.DpsPaymentDialog
import com.example.ui.components.ExtendDueDateDialog
import com.example.ui.components.ProjectEntryDialog
import com.example.ui.components.SettlePersonDialog
import com.example.ui.components.TransferDialog
import com.example.ui.screens.DailyScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.FundFlowViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FundFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                FundFlowApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundFlowApp(viewModel: FundFlowViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog state holders
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var settlingPerson by remember { mutableStateOf<Person?>(null) }
    var extendingPerson by remember { mutableStateOf<Person?>(null) }
    var showAddDpsDialog by remember { mutableStateOf(false) }
    var payingDps by remember { mutableStateOf<DpsScheme?>(null) }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var projectForEntry by remember { mutableStateOf<Project?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "FundFlow",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    FilterChip(
                        selected = uiState.isEditMode,
                        onClick = { viewModel.toggleEditMode() },
                        label = {
                            Text(
                                if (uiState.isEditMode) "সম্পাদনা অন" else "সম্পাদনা",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (uiState.isEditMode) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("edit_mode_toggle")
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation")
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = uiState.selectedTab == tab
                    val icon = when (tab) {
                        AppTab.HOME -> Icons.Default.Home
                        AppTab.DAILY -> Icons.Default.CalendarMonth
                        AppTab.LEDGER -> Icons.Default.MenuBook
                        AppTab.SETTINGS -> Icons.Default.Settings
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setTab(tab) },
                        icon = {
                            if (tab == AppTab.HOME && uiState.pendingTransactions.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text("${uiState.pendingTransactions.size}") }
                                    }
                                ) {
                                    Icon(icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(icon, contentDescription = tab.title)
                            }
                        },
                        label = {
                            Text(
                                when (tab) {
                                    AppTab.HOME -> "হোম"
                                    AppTab.DAILY -> "দৈনিক"
                                    AppTab.LEDGER -> "খাতা ও স্কিম"
                                    AppTab.SETTINGS -> "সেটিংস"
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                AppTab.HOME -> HomeScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onOpenTransfer = { showTransferDialog = true },
                    onOpenAddAccount = { showAddAccountDialog = true }
                )
                AppTab.DAILY -> DailyScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
                AppTab.LEDGER -> LedgerScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onOpenAddPerson = { showAddPersonDialog = true },
                    onOpenSettlePerson = { settlingPerson = it },
                    onOpenExtendPerson = { extendingPerson = it },
                    onOpenAddDps = { showAddDpsDialog = true },
                    onOpenPayDps = { payingDps = it },
                    onOpenAddProject = { showAddProjectDialog = true },
                    onOpenProjectEntry = { projectForEntry = it }
                )
                AppTab.SETTINGS -> SettingsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onOpenAddAccount = { showAddAccountDialog = true }
                )
            }
        }
    }

    // Dialogs
    if (showTransferDialog) {
        TransferDialog(
            accounts = uiState.accounts,
            onDismiss = { showTransferDialog = false },
            onConfirm = { from, to, amount, fee, date, note ->
                viewModel.transfer(from, to, amount, fee, date, note)
                showTransferDialog = false
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, initBal ->
                viewModel.addAccount(name, initBal)
                showAddAccountDialog = false
            }
        )
    }

    if (showAddPersonDialog) {
        AddPersonDialog(
            accounts = uiState.accounts,
            onDismiss = { showAddPersonDialog = false },
            onConfirm = { name, phone, type, amount, accountName, txDate, dueDate ->
                viewModel.addPerson(name, phone, type, amount, accountName, txDate, dueDate)
                showAddPersonDialog = false
            }
        )
    }

    if (settlingPerson != null) {
        SettlePersonDialog(
            person = settlingPerson!!,
            accounts = uiState.accounts,
            onDismiss = { settlingPerson = null },
            onConfirm = { amount, accountName, date ->
                viewModel.settlePerson(settlingPerson!!, amount, accountName, date)
                settlingPerson = null
            }
        )
    }

    if (extendingPerson != null) {
        ExtendDueDateDialog(
            person = extendingPerson!!,
            onDismiss = { extendingPerson = null },
            onConfirm = { newDueDate ->
                viewModel.extendPersonDueDate(extendingPerson!!, newDueDate)
                extendingPerson = null
            }
        )
    }

    if (showAddDpsDialog) {
        AddDpsDialog(
            onDismiss = { showAddDpsDialog = false },
            onConfirm = { bankName, monthlyAmount, dayOfMonth, startDate, durationYears ->
                viewModel.addDpsScheme(bankName, monthlyAmount, dayOfMonth, startDate, durationYears)
                showAddDpsDialog = false
            }
        )
    }

    if (payingDps != null) {
        DpsPaymentDialog(
            dps = payingDps!!,
            accounts = uiState.accounts,
            onDismiss = { payingDps = null },
            onConfirm = { accountName, amount, date ->
                viewModel.payDpsInstallment(payingDps!!, accountName, amount, date)
                payingDps = null
            }
        )
    }

    if (showAddProjectDialog) {
        AddProjectDialog(
            onDismiss = { showAddProjectDialog = false },
            onConfirm = { name ->
                viewModel.createProject(name)
                showAddProjectDialog = false
            }
        )
    }

    if (projectForEntry != null) {
        ProjectEntryDialog(
            project = projectForEntry!!,
            accounts = uiState.accounts,
            onDismiss = { projectForEntry = null },
            onConfirm = { type, accountName, amount, date, desc ->
                viewModel.addProjectEntry(projectForEntry!!.id, type, accountName, amount, date, desc)
                projectForEntry = null
            }
        )
    }
}
