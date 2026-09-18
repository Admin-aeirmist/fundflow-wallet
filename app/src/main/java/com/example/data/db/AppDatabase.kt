package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Account
import com.example.data.model.DpsScheme
import com.example.data.model.Person
import com.example.data.model.Project
import com.example.data.model.ProjectEntry
import com.example.data.model.Transaction

@Database(
    entities = [
        Account::class,
        Transaction::class,
        Person::class,
        DpsScheme::class,
        Project::class,
        ProjectEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fundFlowDao(): FundFlowDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fundflow_app_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
