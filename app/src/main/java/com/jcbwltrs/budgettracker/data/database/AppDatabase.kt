package com.jcbwltrs.budgettracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jcbwltrs.budgettracker.data.dao.*
import com.jcbwltrs.budgettracker.data.model.*

@Database(
    entities = [
        Category::class,
        Transaction::class,
        Income::class,
        MonthlyBudget::class,
        MonthlyCategorySpending::class
    ],
    version = 7, // Reverted back to version 7
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun incomeDao(): IncomeDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun monthlyCategorySpendingDao(): MonthlyCategorySpendingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE monthly_budgets ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS monthly_category_spending (
                        categoryId INTEGER NOT NULL,
                        monthYear TEXT NOT NULL,
                        spent REAL NOT NULL DEFAULT 0.0,
                        PRIMARY KEY(categoryId, monthYear),
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_monthly_category_spending_categoryId ON monthly_category_spending(categoryId)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val currentMonth = java.time.YearMonth.now().toString()
                database.execSQL("""
                    INSERT OR REPLACE INTO monthly_category_spending (categoryId, monthYear, spent)
                    SELECT id, '$currentMonth', spent
                    FROM categories
                    WHERE spent > 0
                """)

                database.execSQL("""
                    CREATE TABLE categories_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        budget REAL NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                """)

                database.execSQL("""
                    INSERT INTO categories_new (id, name, budget, isCompleted, sortOrder)
                    SELECT id, name, budget, isCompleted, sortOrder FROM categories
                """)

                database.execSQL("DROP TABLE categories")
                database.execSQL("ALTER TABLE categories_new RENAME TO categories")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS monthly_budgets (
                        categoryId INTEGER NOT NULL,
                        monthYear TEXT NOT NULL,
                        budget REAL NOT NULL DEFAULT 0.0,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(categoryId, monthYear),
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_monthly_budgets_categoryId ON monthly_budgets(categoryId)")

                val currentMonth = java.time.YearMonth.now().toString()
                database.execSQL("""
                    INSERT INTO monthly_budgets (categoryId, monthYear, budget)
                    SELECT id, '$currentMonth', budget
                    FROM categories
                """)

                database.execSQL("""
                    CREATE TABLE categories_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                """)

                database.execSQL("""
                    INSERT INTO categories_new (id, name, isCompleted, sortOrder)
                    SELECT id, name, isCompleted, sortOrder FROM categories
                """)

                database.execSQL("DROP TABLE categories")
                database.execSQL("ALTER TABLE categories_new RENAME TO categories")
            }
        }

        private val MIGRATION_8_7 = object : Migration(8, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE categories_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                """)
                database.execSQL("""
                    INSERT INTO categories_new (id, name, isCompleted, sortOrder)
                    SELECT id, name, isCompleted, sortOrder FROM categories
                """)
                database.execSQL("DROP TABLE categories")
                database.execSQL("ALTER TABLE categories_new RENAME TO categories")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_database"
                )
                    .addMigrations(
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_8_7 // Downgrade migration from 8 to 7
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
