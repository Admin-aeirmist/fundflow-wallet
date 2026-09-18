package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Account
import com.example.data.model.DpsScheme
import com.example.data.model.Person
import com.example.data.model.Project
import com.example.data.model.ProjectEntry
import com.example.data.model.Transaction
import com.example.data.repository.FundFlowRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    HOME("হোম ড্যাশবোর্ড"),
    DAILY("দৈনিক হিসাব"),
    LEDGER("খাতা ও স্কিম"),
    SETTINGS("সেটিংস ও ব্যাকআপ")
}

enum class LedgerSubTab(val title: String) {
    KHATA("খাতা"),
    DPS("ডিপিএস"),
    PROJECTS("প্রজেক্ট")
}

data class FundFlowUiState(
    val selectedTab: AppTab = AppTab.HOME,
    val selectedLedgerSubTab: LedgerSubTab = LedgerSubTab.KHATA,
    val isEditMode: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val accountBalances: Map<String, Double> = emptyMap(),
    val transactions: List<Transaction> = emptyList(),
    val pendingTransactions: List<Transaction> = emptyList(),
    val persons: List<Person> = emptyList(),
    val dpsSchemes: List<DpsScheme> = emptyList(),
    val projects: List<Project> = emptyList(),
    val projectEntries: List<ProjectEntry> = emptyList(),
    val projectBalances: Map<Long, ProjectSummary> = emptyMap(),
    // Metrics
    val totalNetLiquidCash: Double = 0.0,
    val totalPendingBills: Double = 0.0,
    val totalDpsAccumulated: Double = 0.0,
    val totalReceivable: Double = 0.0,
    val totalPayable: Double = 0.0,
    val totalProjectsNetProfit: Double = 0.0,
    // Sync & Backup
    val googleSheetsUrl: String = FundFlowRepository.DEFAULT_SHEETS_URL,
    val cloudPin: String = "",
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    // Daily screen filters
    val transactionFilterType: String = "ALL", // ALL, expense, income, pending
    val transactionSearchQuery: String = "",
    // Khata screen filters
    val personFilterType: String = "ALL", // ALL, receivable, payable, overdue
    val personSearchQuery: String = "",
    val isLoading: Boolean = false
)

data class ProjectSummary(
    val income: Double,
    val expense: Double,
    val net: Double
)

class FundFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FundFlowRepository

    private val _uiState = MutableStateFlow(FundFlowUiState())
    val uiState: StateFlow<FundFlowUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FundFlowRepository(db.fundFlowDao(), application)

        _uiState.value = _uiState.value.copy(
            googleSheetsUrl = repository.getGoogleSheetsUrl(),
            cloudPin = repository.getCloudPin()
        )

        viewModelScope.launch {
            repository.seedDefaultsIfNeeded()
        }

        // Combine repository streams to compute unified UI state
        viewModelScope.launch {
            combine(
                repository.accounts,
                repository.transactions,
                repository.persons,
                repository.dpsSchemes,
                repository.projects,
                repository.projectEntries
            ) { args: Array<*> ->
                @Suppress("UNCHECKED_CAST")
                val accounts = args[0] as List<Account>
                @Suppress("UNCHECKED_CAST")
                val transactions = args[1] as List<Transaction>
                @Suppress("UNCHECKED_CAST")
                val persons = args[2] as List<Person>
                @Suppress("UNCHECKED_CAST")
                val dpsSchemes = args[3] as List<DpsScheme>
                @Suppress("UNCHECKED_CAST")
                val projects = args[4] as List<Project>
                @Suppress("UNCHECKED_CAST")
                val projectEntries = args[5] as List<ProjectEntry>
                computeState(accounts, transactions, persons, dpsSchemes, projects, projectEntries)
            }.collect { computed ->
                _uiState.value = _uiState.value.copy(
                    accounts = computed.accounts,
                    accountBalances = computed.accountBalances,
                    transactions = computed.transactions,
                    pendingTransactions = computed.pendingTransactions,
                    persons = computed.persons,
                    dpsSchemes = computed.dpsSchemes,
                    projects = computed.projects,
                    projectEntries = computed.projectEntries,
                    projectBalances = computed.projectBalances,
                    totalNetLiquidCash = computed.totalNetLiquidCash,
                    totalPendingBills = computed.totalPendingBills,
                    totalDpsAccumulated = computed.totalDpsAccumulated,
                    totalReceivable = computed.totalReceivable,
                    totalPayable = computed.totalPayable,
                    totalProjectsNetProfit = computed.totalProjectsNetProfit
                )
            }
        }
    }

    private fun computeState(
        accounts: List<Account>,
        transactions: List<Transaction>,
        persons: List<Person>,
        dpsSchemes: List<DpsScheme>,
        projects: List<Project>,
        projectEntries: List<ProjectEntry>
    ): ComputedData {
        // Compute account balances: account.initialBalance + income - expense
        val accMap = mutableMapOf<String, Double>()
        accounts.forEach { acc ->
            accMap[acc.name] = acc.initialBalance
        }

        transactions.forEach { tx ->
            val cur = accMap[tx.accountName] ?: 0.0
            when (tx.type) {
                "income" -> accMap[tx.accountName] = cur + tx.amount
                "expense" -> accMap[tx.accountName] = cur - tx.amount
            }
        }

        val totalNet = accMap.values.sum()
        val pendingList = transactions.filter { it.type == "pending" }
        val totalPending = pendingList.sumOf { it.amount }
        val totalDps = dpsSchemes.sumOf { it.totalDeposited }

        val activePersons = persons.filter { !it.isSettled }
        val totalRec = activePersons.filter { it.type == "receivable" }.sumOf { it.balance }
        val totalPay = activePersons.filter { it.type == "payable" }.sumOf { it.balance }

        // Compute project summaries
        val projSummaryMap = mutableMapOf<Long, ProjectSummary>()
        projects.forEach { proj ->
            val entries = projectEntries.filter { it.projectId == proj.id }
            val inc = entries.filter { it.type == "income" }.sumOf { it.amount }
            val exp = entries.filter { it.type == "expense" }.sumOf { it.amount }
            projSummaryMap[proj.id] = ProjectSummary(income = inc, expense = exp, net = inc - exp)
        }
        val totalProjectsNetProfit = projSummaryMap.values.sumOf { it.net }

        return ComputedData(
            accounts = accounts,
            accountBalances = accMap,
            transactions = transactions,
            pendingTransactions = pendingList,
            persons = persons,
            dpsSchemes = dpsSchemes,
            projects = projects,
            projectEntries = projectEntries,
            projectBalances = projSummaryMap,
            totalNetLiquidCash = totalNet,
            totalPendingBills = totalPending,
            totalDpsAccumulated = totalDps,
            totalReceivable = totalRec,
            totalPayable = totalPay,
            totalProjectsNetProfit = totalProjectsNetProfit
        )
    }

    private data class ComputedData(
        val accounts: List<Account>,
        val accountBalances: Map<String, Double>,
        val transactions: List<Transaction>,
        val pendingTransactions: List<Transaction>,
        val persons: List<Person>,
        val dpsSchemes: List<DpsScheme>,
        val projects: List<Project>,
        val projectEntries: List<ProjectEntry>,
        val projectBalances: Map<Long, ProjectSummary>,
        val totalNetLiquidCash: Double,
        val totalPendingBills: Double,
        val totalDpsAccumulated: Double,
        val totalReceivable: Double,
        val totalPayable: Double,
        val totalProjectsNetProfit: Double
    )

    // Navigation & View Toggles
    fun setTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setLedgerSubTab(subTab: LedgerSubTab) {
        _uiState.value = _uiState.value.copy(selectedLedgerSubTab = subTab)
    }

    fun toggleEditMode() {
        val newMode = !_uiState.value.isEditMode
        _uiState.value = _uiState.value.copy(isEditMode = newMode)
    }

    fun setTransactionFilter(type: String) {
        _uiState.value = _uiState.value.copy(transactionFilterType = type)
    }

    fun setTransactionSearch(query: String) {
        _uiState.value = _uiState.value.copy(transactionSearchQuery = query)
    }

    fun setPersonFilter(type: String) {
        _uiState.value = _uiState.value.copy(personFilterType = type)
    }

    fun setPersonSearch(query: String) {
        _uiState.value = _uiState.value.copy(personSearchQuery = query)
    }

    // Actions
    fun addTransaction(type: String, account: String, amount: Double, date: String, desc: String) {
        viewModelScope.launch {
            if (account.isBlank() || amount <= 0) {
                _toastEvent.emit("সঠিক পরিমাণ ও মাধ্যম নির্বাচন করুন")
                return@launch
            }
            repository.addTransaction(type, account, amount, date, desc)
            _toastEvent.emit("নতুন লেনদেন সফলভাবে সংরক্ষিত হয়েছে")
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _toastEvent.emit("লেনদেনটি মুছে ফেলা হয়েছে")
        }
    }

    fun settlePending(transaction: Transaction, accountName: String) {
        viewModelScope.launch {
            repository.settlePending(transaction, accountName)
            _toastEvent.emit("পেন্ডিং বিল পরিশোধ হিসেবে গণ্য করা হয়েছে")
        }
    }

    fun transfer(fromAccount: String, toAccount: String, amount: Double, fee: Double, date: String, note: String) {
        viewModelScope.launch {
            if (fromAccount == toAccount) {
                _toastEvent.emit("উৎস ও গন্তব্য মাধ্যম একই হতে পারে না")
                return@launch
            }
            if (amount <= 0) {
                _toastEvent.emit("সঠিক টাকার পরিমাণ দিন")
                return@launch
            }
            repository.transfer(fromAccount, toAccount, amount, fee, date, note)
            _toastEvent.emit("ট্রান্সফার সফলভাবে সম্পন্ন হয়েছে")
        }
    }

    fun addPerson(
        name: String,
        phone: String,
        type: String,
        amount: Double,
        accountName: String,
        txDate: String,
        dueDate: String
    ) {
        viewModelScope.launch {
            if (name.isBlank() || amount <= 0) {
                _toastEvent.emit("ব্যক্তির নাম ও টাকার পরিমাণ আবশ্যক")
                return@launch
            }
            repository.addPerson(name, phone, type, amount, accountName, txDate, dueDate, recordInAccount = true)
            _toastEvent.emit("খাতায় নতুন ব্যক্তি যোগ হয়েছে")
        }
    }

    fun settlePerson(person: Person, paidAmount: Double, accountName: String, date: String) {
        viewModelScope.launch {
            if (paidAmount <= 0) {
                _toastEvent.emit("সঠিক পরিশোধের পরিমাণ দিন")
                return@launch
            }
            repository.settlePerson(person, paidAmount, accountName, date)
            _toastEvent.emit("খাতা নিষ্পত্তি সফলভাবে সংরক্ষিত হয়েছে")
        }
    }

    fun extendPersonDueDate(person: Person, newDueDate: String) {
        viewModelScope.launch {
            repository.extendPersonDueDate(person, newDueDate)
            _toastEvent.emit("পরিশোধের তারিখ আপডেট করা হয়েছে")
        }
    }

    fun deletePerson(person: Person) {
        viewModelScope.launch {
            repository.deletePerson(person)
            _toastEvent.emit("ব্যক্তি হিসাব মুছে ফেলা হয়েছে")
        }
    }

    fun addDpsScheme(bankName: String, monthlyAmount: Double, dayOfMonth: Int, startDate: String, durationYears: Int) {
        viewModelScope.launch {
            if (bankName.isBlank() || monthlyAmount <= 0) {
                _toastEvent.emit("ব্যাংক নাম ও মাসিক কিস্তি আবশ্যক")
                return@launch
            }
            repository.addDpsScheme(bankName, monthlyAmount, dayOfMonth, startDate, durationYears)
            _toastEvent.emit("নতুন ডিপিএস স্কিম তৈরি হয়েছে")
        }
    }

    fun payDpsInstallment(dps: DpsScheme, accountName: String, amount: Double, date: String) {
        viewModelScope.launch {
            if (amount <= 0) {
                _toastEvent.emit("সঠিক কিস্তির পরিমাণ দিন")
                return@launch
            }
            repository.payDpsInstallment(dps, accountName, amount, date)
            _toastEvent.emit("${dps.bankName} কিস্তি জমা হয়েছে")
        }
    }

    fun deleteDps(dps: DpsScheme) {
        viewModelScope.launch {
            repository.deleteDps(dps)
            _toastEvent.emit("ডিপিএস স্কিম মুছে ফেলা হয়েছে")
        }
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _toastEvent.emit("প্রজেক্টের নাম দিন")
                return@launch
            }
            repository.createProject(name)
            _toastEvent.emit("নতুন প্রজেক্ট তৈরি হয়েছে")
        }
    }

    fun addProjectEntry(projectId: Long, type: String, accountName: String, amount: Double, date: String, desc: String) {
        viewModelScope.launch {
            if (amount <= 0) {
                _toastEvent.emit("সঠিক পরিমাণ দিন")
                return@launch
            }
            repository.addProjectEntry(projectId, type, accountName, amount, date, desc)
            _toastEvent.emit("প্রজেক্ট এন্ট্রি সংরক্ষিত হয়েছে")
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
            _toastEvent.emit("প্রজেক্ট মুছে ফেলা হয়েছে")
        }
    }

    fun deleteProjectEntry(entry: ProjectEntry) {
        viewModelScope.launch {
            repository.deleteProjectEntry(entry)
            _toastEvent.emit("এন্ট্রি মুছে ফেলা হয়েছে")
        }
    }

    fun addAccount(name: String, initialBalance: Double = 0.0) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _toastEvent.emit("মাধ্যমের নাম দিন")
                return@launch
            }
            if (_uiState.value.accounts.any { it.name.equals(name.trim(), ignoreCase = true) }) {
                _toastEvent.emit("এই মাধ্যম ইতিমধ্যে বিদ্যমান")
                return@launch
            }
            repository.addAccount(name, initialBalance)
            _toastEvent.emit("নতুন মাধ্যম যোগ হয়েছে")
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            if (account.isDefault) {
                _toastEvent.emit("ডিফল্ট মাধ্যম মুছে ফেলা সম্ভব নয়")
                return@launch
            }
            repository.deleteAccount(account)
            _toastEvent.emit("মাধ্যম মুছে ফেলা হয়েছে")
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            if (transaction.amount <= 0) {
                _toastEvent.emit("সঠিক পরিমাণ দিন")
                return@launch
            }
            if (transaction.description.isBlank()) {
                _toastEvent.emit("বিবরণ আবশ্যক")
                return@launch
            }
            repository.updateTransaction(transaction)
            _toastEvent.emit("লেনদেন সফলভাবে আপডেট করা হয়েছে")
        }
    }

    fun editProjectName(projectId: Long, newName: String) {
        viewModelScope.launch {
            if (newName.isBlank()) {
                _toastEvent.emit("প্রজেক্টের নাম দিন")
                return@launch
            }
            repository.updateProjectName(projectId, newName)
            _toastEvent.emit("প্রজেক্টের নাম পরিবর্তিত হয়েছে")
        }
    }

    fun editProjectEntry(entry: ProjectEntry) {
        viewModelScope.launch {
            if (entry.amount <= 0) {
                _toastEvent.emit("সঠিক পরিমাণ দিন")
                return@launch
            }
            if (entry.description.isBlank()) {
                _toastEvent.emit("বিবরণ আবশ্যক")
                return@launch
            }
            repository.updateProjectEntry(entry)
            _toastEvent.emit("প্রজেক্ট এন্ট্রি আপডেট করা হয়েছে")
        }
    }

    fun saveGoogleSheetsUrl(url: String) {
        repository.setGoogleSheetsUrl(url)
        _uiState.value = _uiState.value.copy(googleSheetsUrl = url)
    }

    fun saveCloudPin(pin: String) {
        repository.setCloudPin(pin)
        _uiState.value = _uiState.value.copy(cloudPin = pin)
    }

    fun syncAllToGoogleSheets(customUrl: String? = null) {
        val targetUrl = customUrl?.takeIf { it.isNotBlank() } ?: _uiState.value.googleSheetsUrl
        if (targetUrl.isBlank()) {
            viewModelScope.launch { _toastEvent.emit("গুগল শিট Web App URL দিন") }
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = "গুগল শিটে ব্যাকআপ পাঠানো হচ্ছে...")
            val result = repository.exportAllToGoogleSheets(targetUrl)
            _uiState.value = _uiState.value.copy(isSyncing = false, syncMessage = null)
            result.fold(
                onSuccess = { msg -> _toastEvent.emit(msg) },
                onFailure = { err -> _toastEvent.emit("গুগল শিটে পাঠাতে সমস্যা: ${err.localizedMessage}") }
            )
        }
    }

    fun backupToCloud(pin: String) {
        if (pin.isBlank() || pin.length < 3) {
            viewModelScope.launch { _toastEvent.emit("অনুগ্রহ করে অন্তত ৩-৪ ডিজিটের একটি পিন দিন!") }
            return
        }
        saveCloudPin(pin)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = "ক্লাউডে আপলোড হচ্ছে...")
            val result = repository.backupToCloud(pin)
            _uiState.value = _uiState.value.copy(isSyncing = false, syncMessage = null)
            result.fold(
                onSuccess = { msg -> _toastEvent.emit(msg) },
                onFailure = { err -> _toastEvent.emit("ক্লাউড ব্যাকআপ ত্রুটি: ${err.localizedMessage}") }
            )
        }
    }

    fun restoreFromCloud(pin: String) {
        if (pin.isBlank()) {
            viewModelScope.launch { _toastEvent.emit("অনুগ্রহ করে আপনার পিন লিখুন!") }
            return
        }
        saveCloudPin(pin)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = "ক্লাউড থেকে তথ্য নামানো হচ্ছে...")
            val result = repository.restoreFromCloud(pin)
            _uiState.value = _uiState.value.copy(isSyncing = false, syncMessage = null)
            result.fold(
                onSuccess = { msg -> _toastEvent.emit(msg) },
                onFailure = { err -> _toastEvent.emit("ক্লাউড রিস্টোর ব্যর্থ: ${err.localizedMessage}") }
            )
        }
    }

    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportJson()
            onReady(json)
            _toastEvent.emit("ব্যাকআপ ফাইল প্রস্তুত করা হয়েছে")
        }
    }

    fun importBackupJson(jsonString: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val res = repository.importDataFromJson(jsonString)
            _uiState.value = _uiState.value.copy(isSyncing = false)
            res.fold(
                onSuccess = { msg -> _toastEvent.emit(msg) },
                onFailure = { err -> _toastEvent.emit("ইমপোর্ট ব্যর্থ: ${err.localizedMessage}") }
            )
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.seedDefaultsIfNeeded()
            _toastEvent.emit("সব ডেটা রিসেট করা হয়েছে")
        }
    }
}
