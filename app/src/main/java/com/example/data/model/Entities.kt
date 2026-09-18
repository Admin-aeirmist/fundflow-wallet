package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val initialBalance: Double = 0.0
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "income", "expense", "pending"
    val accountName: String,
    val amount: Double,
    val date: String, // "yyyy-MM-dd"
    val description: String,
    val transferGroupId: String? = null,
    val relatedPersonId: Long? = null,
    val relatedDpsId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val type: String, // "receivable" (আমি পাবো), "payable" (সে পাবে)
    val balance: Double,
    val initialAmount: Double,
    val accountName: String,
    val txDate: String,
    val dueDate: String,
    val isSettled: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "dps_schemes")
data class DpsScheme(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String,
    val monthlyAmount: Double,
    val dayOfMonth: Int,
    val startDate: String,
    val durationYears: Int,
    val totalDeposited: Double = 0.0,
    val totalInstallmentsPaid: Int = 0,
    val nextDueDate: String
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: String
)

@Entity(tableName = "project_entries")
data class ProjectEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val type: String, // "income", "expense"
    val accountName: String,
    val amount: Double,
    val date: String,
    val description: String
)
