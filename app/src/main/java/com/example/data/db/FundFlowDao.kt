package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Account
import com.example.data.model.DpsScheme
import com.example.data.model.Person
import com.example.data.model.Project
import com.example.data.model.ProjectEntry
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FundFlowDao {
    // Accounts
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    // Persons (Khata)
    @Query("SELECT * FROM persons ORDER BY isSettled ASC, dueDate ASC, id DESC")
    fun getPersons(): Flow<List<Person>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: Person): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersons(persons: List<Person>)

    @Update
    suspend fun updatePerson(person: Person)

    @Delete
    suspend fun deletePerson(person: Person)

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deletePersonById(id: Long)

    // DPS Schemes
    @Query("SELECT * FROM dps_schemes ORDER BY nextDueDate ASC, id DESC")
    fun getDpsSchemes(): Flow<List<DpsScheme>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDps(dps: DpsScheme): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDpsList(list: List<DpsScheme>)

    @Update
    suspend fun updateDps(dps: DpsScheme)

    @Delete
    suspend fun deleteDps(dps: DpsScheme)

    @Query("DELETE FROM dps_schemes WHERE id = :id")
    suspend fun deleteDpsById(id: Long)

    // Projects
    @Query("SELECT * FROM projects ORDER BY id DESC")
    fun getProjects(): Flow<List<Project>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<Project>)

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    // Project Entries
    @Query("SELECT * FROM project_entries ORDER BY date DESC, id DESC")
    fun getProjectEntries(): Flow<List<ProjectEntry>>

    @Query("SELECT * FROM project_entries WHERE projectId = :projectId ORDER BY date DESC, id DESC")
    fun getEntriesForProject(projectId: Long): Flow<List<ProjectEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectEntry(entry: ProjectEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectEntries(entries: List<ProjectEntry>)

    @Update
    suspend fun updateProjectEntry(entry: ProjectEntry)

    @Delete
    suspend fun deleteProjectEntry(entry: ProjectEntry)

    @Query("DELETE FROM project_entries WHERE projectId = :projectId")
    suspend fun deleteEntriesForProject(projectId: Long)

    // Maintenance / Reset
    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM persons")
    suspend fun clearPersons()

    @Query("DELETE FROM dps_schemes")
    suspend fun clearDps()

    @Query("DELETE FROM projects")
    suspend fun clearProjects()

    @Query("DELETE FROM project_entries")
    suspend fun clearProjectEntries()

    @Query("DELETE FROM accounts WHERE isDefault = 0")
    suspend fun clearCustomAccounts()
}
