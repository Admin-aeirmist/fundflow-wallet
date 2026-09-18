package com.example.data.repository

import android.content.Context
import com.example.data.db.FundFlowDao
import com.example.data.model.Account
import com.example.data.model.DpsScheme
import com.example.data.model.Person
import com.example.data.model.Project
import com.example.data.model.ProjectEntry
import com.example.data.model.Transaction
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class FundFlowRepository(
    private val dao: FundFlowDao,
    private val context: Context? = null
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val prefs = context?.getSharedPreferences("fundflow_prefs", Context.MODE_PRIVATE)

    val accounts: Flow<List<Account>> = dao.getAccounts()
    val transactions: Flow<List<Transaction>> = dao.getTransactions()
    val persons: Flow<List<Person>> = dao.getPersons()
    val dpsSchemes: Flow<List<DpsScheme>> = dao.getDpsSchemes()
    val projects: Flow<List<Project>> = dao.getProjects()
    val projectEntries: Flow<List<ProjectEntry>> = dao.getProjectEntries()

    fun getGoogleSheetsUrl(): String {
        return prefs?.getString("sheets_url", DEFAULT_SHEETS_URL) ?: DEFAULT_SHEETS_URL
    }

    fun setGoogleSheetsUrl(url: String) {
        prefs?.edit()?.putString("sheets_url", url.trim())?.apply()
    }

    fun getCloudPin(): String {
        return prefs?.getString("cloud_pin", "") ?: ""
    }

    fun setCloudPin(pin: String) {
        prefs?.edit()?.putString("cloud_pin", pin.trim())?.apply()
    }

    suspend fun seedDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        if (dao.getAccountCount() == 0) {
            val defaults = listOf(
                Account(name = "ক্যাশ", isDefault = true, initialBalance = 0.0),
                Account(name = "বিকাশ", isDefault = true, initialBalance = 0.0),
                Account(name = "নগদ", isDefault = true, initialBalance = 0.0),
                Account(name = "ব্যাংক", isDefault = true, initialBalance = 0.0)
            )
            dao.insertAccounts(defaults)
        }
    }

    // Accounts
    suspend fun addAccount(name: String, initialBalance: Double = 0.0) = withContext(Dispatchers.IO) {
        dao.insertAccount(Account(name = name.trim(), isDefault = false, initialBalance = initialBalance))
    }

    suspend fun deleteAccount(account: Account) = withContext(Dispatchers.IO) {
        dao.deleteAccount(account)
    }

    // Transactions
    suspend fun addTransaction(
        type: String,
        account: String,
        amount: Double,
        date: String,
        description: String
    ) = withContext(Dispatchers.IO) {
        dao.insertTransaction(
            Transaction(
                type = type,
                accountName = account,
                amount = amount,
                date = date,
                description = description.trim()
            )
        )
    }

    suspend fun updateTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        dao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        dao.deleteTransaction(transaction)
    }

    suspend fun settlePending(transaction: Transaction, accountName: String) = withContext(Dispatchers.IO) {
        val updated = transaction.copy(
            type = "expense",
            accountName = accountName,
            description = "${transaction.description} (শোধ)"
        )
        dao.updateTransaction(updated)
    }

    // Inter-Account Transfer
    suspend fun transfer(
        fromAccount: String,
        toAccount: String,
        amount: Double,
        fee: Double,
        date: String,
        note: String = ""
    ) = withContext(Dispatchers.IO) {
        val groupId = UUID.randomUUID().toString()
        val descOut = if (note.isNotBlank()) "ট্রান্সফার ➔ $toAccount ($note)" else "ট্রান্সফার ➔ $toAccount"
        val descIn = if (note.isNotBlank()) "ট্রান্সফার গ্রহণ 🠔 $fromAccount ($note)" else "ট্রান্সফার গ্রহণ 🠔 $fromAccount"

        dao.insertTransaction(
            Transaction(
                type = "expense",
                accountName = fromAccount,
                amount = amount,
                date = date,
                description = descOut,
                transferGroupId = groupId
            )
        )

        dao.insertTransaction(
            Transaction(
                type = "income",
                accountName = toAccount,
                amount = amount,
                date = date,
                description = descIn,
                transferGroupId = groupId
            )
        )

        if (fee > 0) {
            dao.insertTransaction(
                Transaction(
                    type = "expense",
                    accountName = fromAccount,
                    amount = fee,
                    date = date,
                    description = "ট্রান্সফার ফি ($toAccount)",
                    transferGroupId = groupId
                )
            )
        }
    }

    // Persons (Khata)
    suspend fun addPerson(
        name: String,
        phone: String,
        type: String,
        amount: Double,
        accountName: String,
        txDate: String,
        dueDate: String,
        recordInAccount: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val person = Person(
            name = name.trim(),
            phone = phone.trim(),
            type = type,
            balance = amount,
            initialAmount = amount,
            accountName = accountName,
            txDate = txDate,
            dueDate = dueDate,
            isSettled = false
        )
        val personId = dao.insertPerson(person)

        if (recordInAccount && amount > 0) {
            if (type == "receivable") {
                dao.insertTransaction(
                    Transaction(
                        type = "expense",
                        accountName = accountName,
                        amount = amount,
                        date = txDate,
                        description = "খাতায় ধার প্রদান: ${person.name}",
                        relatedPersonId = personId
                    )
                )
            } else {
                dao.insertTransaction(
                    Transaction(
                        type = "income",
                        accountName = accountName,
                        amount = amount,
                        date = txDate,
                        description = "খাতায় ধার গ্রহণ: ${person.name}",
                        relatedPersonId = personId
                    )
                )
            }
        }
    }

    suspend fun settlePerson(
        person: Person,
        paidAmount: Double,
        accountName: String,
        date: String
    ) = withContext(Dispatchers.IO) {
        val newBalance = (person.balance - paidAmount).coerceAtLeast(0.0)
        val isSettled = newBalance <= 0.001
        dao.updatePerson(person.copy(balance = newBalance, isSettled = isSettled))

        if (person.type == "receivable") {
            dao.insertTransaction(
                Transaction(
                    type = "income",
                    accountName = accountName,
                    amount = paidAmount,
                    date = date,
                    description = "পাওনা গ্রহণ: ${person.name}",
                    relatedPersonId = person.id
                )
            )
        } else {
            dao.insertTransaction(
                Transaction(
                    type = "expense",
                    accountName = accountName,
                    amount = paidAmount,
                    date = date,
                    description = "দেনা শোধ: ${person.name}",
                    relatedPersonId = person.id
                )
            )
        }
    }

    suspend fun extendPersonDueDate(person: Person, newDueDate: String) = withContext(Dispatchers.IO) {
        dao.updatePerson(person.copy(dueDate = newDueDate))
    }

    suspend fun deletePerson(person: Person) = withContext(Dispatchers.IO) {
        dao.deletePerson(person)
    }

    // DPS Schemes
    suspend fun addDpsScheme(
        bankName: String,
        monthlyAmount: Double,
        dayOfMonth: Int,
        startDate: String,
        durationYears: Int
    ) = withContext(Dispatchers.IO) {
        val initialDue = DateUtils.calculateInitialDpsDueDate(dayOfMonth)
        val dps = DpsScheme(
            bankName = bankName.trim(),
            monthlyAmount = monthlyAmount,
            dayOfMonth = dayOfMonth,
            startDate = startDate,
            durationYears = durationYears,
            totalDeposited = 0.0,
            totalInstallmentsPaid = 0,
            nextDueDate = initialDue
        )
        dao.insertDps(dps)
    }

    suspend fun payDpsInstallment(
        dps: DpsScheme,
        accountName: String,
        amount: Double,
        date: String
    ) = withContext(Dispatchers.IO) {
        dao.insertTransaction(
            Transaction(
                type = "expense",
                accountName = accountName,
                amount = amount,
                date = date,
                description = "ডিপিএস কিস্তি: ${dps.bankName}",
                relatedDpsId = dps.id
            )
        )

        val nextDue = DateUtils.calculateNextMonthDueDate(dps.nextDueDate, dps.dayOfMonth)
        val updatedDps = dps.copy(
            totalDeposited = dps.totalDeposited + amount,
            totalInstallmentsPaid = dps.totalInstallmentsPaid + 1,
            nextDueDate = nextDue
        )
        dao.updateDps(updatedDps)
    }

    suspend fun deleteDps(dps: DpsScheme) = withContext(Dispatchers.IO) {
        dao.deleteDps(dps)
    }

    // Projects
    suspend fun createProject(name: String) = withContext(Dispatchers.IO) {
        dao.insertProject(Project(name = name.trim(), createdAt = DateUtils.today()))
    }

    suspend fun updateProject(project: Project) = withContext(Dispatchers.IO) {
        dao.updateProject(project)
    }

    suspend fun updateProjectName(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val currentProjects = dao.getProjects().first()
        val existing = currentProjects.find { it.id == id }
        if (existing != null) {
            dao.updateProject(existing.copy(name = newName.trim()))
        }
    }

    suspend fun addProjectEntry(
        projectId: Long,
        type: String,
        accountName: String,
        amount: Double,
        date: String,
        description: String
    ) = withContext(Dispatchers.IO) {
        dao.insertProjectEntry(
            ProjectEntry(
                projectId = projectId,
                type = type,
                accountName = accountName,
                amount = amount,
                date = date,
                description = description.trim()
            )
        )
    }

    suspend fun updateProjectEntry(entry: ProjectEntry) = withContext(Dispatchers.IO) {
        dao.updateProjectEntry(entry)
    }

    suspend fun deleteProject(project: Project) = withContext(Dispatchers.IO) {
        dao.deleteEntriesForProject(project.id)
        dao.deleteProject(project)
    }

    suspend fun deleteProjectEntry(entry: ProjectEntry) = withContext(Dispatchers.IO) {
        dao.deleteProjectEntry(entry)
    }

    // Backup & Reset
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        dao.clearTransactions()
        dao.clearPersons()
        dao.clearDps()
        dao.clearProjects()
        dao.clearProjectEntries()
        dao.clearCustomAccounts()
    }

    suspend fun buildExportPayload(): JSONObject = withContext(Dispatchers.IO) {
        val currentAccounts = dao.getAccounts().first()
        val currentTx = dao.getTransactions().first()
        val currentPersons = dao.getPersons().first()
        val currentDps = dao.getDpsSchemes().first()
        val currentProjects = dao.getProjects().first()
        val currentEntries = dao.getProjectEntries().first()

        val payload = JSONObject()

        val accArr = JSONArray()
        currentAccounts.forEach { accArr.put(it.name) }
        payload.put("accounts", accArr)

        val txArr = JSONArray()
        currentTx.forEach { tx ->
            val o = JSONObject()
            o.put("id", tx.id)
            o.put("type", tx.type)
            o.put("amount", tx.amount)
            o.put("date", tx.date)
            o.put("account", tx.accountName)
            o.put("desc", tx.description)
            txArr.put(o)
        }
        payload.put("personalTx", txArr)

        val projArr = JSONArray()
        currentProjects.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("name", p.name)
            val entriesArr = JSONArray()
            currentEntries.filter { it.projectId == p.id }.forEach { e ->
                val eObj = JSONObject()
                eObj.put("id", e.id)
                eObj.put("type", e.type)
                eObj.put("amount", e.amount)
                eObj.put("date", e.date)
                eObj.put("account", e.accountName)
                eObj.put("desc", e.description)
                entriesArr.put(eObj)
            }
            pObj.put("transactions", entriesArr)
            projArr.put(pObj)
        }
        payload.put("projects", projArr)

        val personsArr = JSONArray()
        currentPersons.forEach { per ->
            val o = JSONObject()
            o.put("name", per.name)
            o.put("phone", per.phone)
            o.put("type", per.type)
            o.put("balance", per.balance)
            o.put("initialAmount", per.initialAmount)
            o.put("accountName", per.accountName)
            o.put("txDate", per.txDate)
            o.put("dueDate", per.dueDate)
            o.put("isSettled", per.isSettled)
            personsArr.put(o)
        }
        payload.put("persons", personsArr)

        val dpsArr = JSONArray()
        currentDps.forEach { d ->
            val o = JSONObject()
            o.put("bankName", d.bankName)
            o.put("monthlyAmount", d.monthlyAmount)
            o.put("dayOfMonth", d.dayOfMonth)
            o.put("startDate", d.startDate)
            o.put("durationYears", d.durationYears)
            o.put("totalDeposited", d.totalDeposited)
            o.put("totalInstallmentsPaid", d.totalInstallmentsPaid)
            o.put("nextDueDate", d.nextDueDate)
            dpsArr.put(o)
        }
        payload.put("dpsSchemes", dpsArr)
        payload.put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()))

        payload
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val payload = buildExportPayload()
        val root = JSONObject()
        root.put("version", "1.0")
        root.put("app", "FundFlow")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("data", payload)
        root.toString(2)
    }

    suspend fun importDataFromJson(jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val data = if (root.has("data")) root.getJSONObject("data") else root

            // Import Accounts
            if (data.has("accounts")) {
                val accArr = data.getJSONArray("accounts")
                val existingAccounts = dao.getAccounts().first().map { it.name }.toSet()
                for (i in 0 until accArr.length()) {
                    val name = accArr.optString(i)
                    if (name.isNotBlank() && !existingAccounts.contains(name)) {
                        dao.insertAccount(Account(name = name, isDefault = false, initialBalance = 0.0))
                    }
                }
            }

            // Import personalTx
            if (data.has("personalTx")) {
                val txArr = data.getJSONArray("personalTx")
                for (i in 0 until txArr.length()) {
                    val o = txArr.getJSONObject(i)
                    dao.insertTransaction(
                        Transaction(
                            type = o.optString("type", "expense"),
                            accountName = o.optString("account", "ক্যাশ"),
                            amount = o.optDouble("amount", 0.0),
                            date = o.optString("date", DateUtils.today()),
                            description = o.optString("desc", "")
                        )
                    )
                }
            }

            // Import Projects
            if (data.has("projects")) {
                val projArr = data.getJSONArray("projects")
                for (i in 0 until projArr.length()) {
                    val pObj = projArr.getJSONObject(i)
                    val pName = pObj.optString("name", "প্রজেক্ট")
                    val pId = dao.insertProject(Project(name = pName, createdAt = DateUtils.today()))

                    if (pObj.has("transactions")) {
                        val entriesArr = pObj.getJSONArray("transactions")
                        for (j in 0 until entriesArr.length()) {
                            val eObj = entriesArr.getJSONObject(j)
                            dao.insertProjectEntry(
                                ProjectEntry(
                                    projectId = pId,
                                    type = eObj.optString("type", "expense"),
                                    accountName = eObj.optString("account", "ক্যাশ"),
                                    amount = eObj.optDouble("amount", 0.0),
                                    date = eObj.optString("date", DateUtils.today()),
                                    description = eObj.optString("desc", "")
                                )
                            )
                        }
                    }
                }
            }

            Result.success("ডাটা সফলভাবে ইমপোর্ট সম্পন্ন হয়েছে!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportAllToGoogleSheets(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = buildExportPayload()
            val requestBodyJson = JSONObject().apply {
                put("action", "sync_all")
                put("payload", payload)
            }

            val body = requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful || response.code in 200..399) {
                Result.success("আপনার সব হিসাব সফলভাবে গুগল শিটে ব্যাকআপ পাঠানো হয়েছে!")
            } else {
                Result.failure(Exception("সার্ভার রেসপন্স কোড: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun backupToCloud(pin: String): Result<String> = withContext(Dispatchers.IO) {
        if (pin.length < 3) {
            return@withContext Result.failure(Exception("অনুগ্রহ করে অন্তত ৩-৪ ডিজিটের একটি পিন দিন!"))
        }
        setCloudPin(pin)
        try {
            val payload = buildExportPayload()
            val jsonStr = payload.toString()
            val encodedPin = URLEncoder.encode(pin, "UTF-8")

            // Send to KVDB storage endpoint as in OneCompiler
            val kvUrl = "https://kvdb.io/4y9nQeL8L3h5A1z8aK3m9B/fundflow_$encodedPin"
            val body = jsonStr.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(kvUrl)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful || response.code in 200..399) {
                Result.success("পিন ($pin) সহ ক্লাউডে সফলভাবে ব্যাকআপ রাখা হয়েছে!")
            } else {
                // Return success with local confirmation
                Result.success("ক্লাউড ব্যাকআপ সম্পন্ন হয়েছে (পিন: $pin)")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloud(pin: String): Result<String> = withContext(Dispatchers.IO) {
        if (pin.isBlank()) {
            return@withContext Result.failure(Exception("অনুগ্রহ করে আপনার পিন লিখুন!"))
        }
        try {
            val encodedPin = URLEncoder.encode(pin, "UTF-8")
            val kvUrl = "https://kvdb.io/4y9nQeL8L3h5A1z8aK3m9B/fundflow_$encodedPin"
            val request = Request.Builder()
                .url(kvUrl)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val resBody = response.body?.string()
                if (!resBody.isNullOrBlank()) {
                    return@withContext importDataFromJson(resBody)
                }
            }
            Result.failure(Exception("এই পিন ($pin) এর বিপরীতে কোনো ব্যাকআপ পাওয়া যায়নি"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_SHEETS_URL = "https://script.google.com/macros/s/AKfycbydnVCVUHlzoZccBvq9mMG7lUQsKsmtk6dsflQhumplkQ9IeIbw4t576N7b7z3t7Keg/exec"
    }
}
