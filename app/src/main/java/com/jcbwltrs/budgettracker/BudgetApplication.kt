package com.jcbwltrs.budgettracker

import android.app.Application
import com.jcbwltrs.budgettracker.data.database.AppDatabase
import com.jcbwltrs.budgettracker.data.model.Category
import com.jcbwltrs.budgettracker.data.model.MonthlyBudget
import com.jcbwltrs.budgettracker.data.preferences.UserPreferences
import com.jcbwltrs.budgettracker.data.repository.CategoryRepository
import com.jcbwltrs.budgettracker.data.repository.TransactionRepository
import com.jcbwltrs.budgettracker.data.repository.IncomeRepository
import com.jcbwltrs.budgettracker.data.repository.MonthlyBudgetRepository
import com.jcbwltrs.budgettracker.ui.SharedMonthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

class BudgetApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    val userPreferences by lazy { UserPreferences(this) }

    // DAOs
    private val categoryDao by lazy { database.categoryDao() }
    private val transactionDao by lazy { database.transactionDao() }
    private val incomeDao by lazy { database.incomeDao() }
    private val monthlyBudgetDao by lazy { database.monthlyBudgetDao() }
    private val monthlyCategorySpendingDao by lazy { database.monthlyCategorySpendingDao() }

    // Repositories
    val transactionRepository by lazy { TransactionRepository(transactionDao) }
    val incomeRepository by lazy { IncomeRepository(incomeDao) }
    val monthlyBudgetRepository by lazy { MonthlyBudgetRepository(monthlyBudgetDao) }
    val categoryRepository by lazy {
        CategoryRepository(
            categoryDao = categoryDao,
            monthlyCategorySpendingDao = monthlyCategorySpendingDao,
            monthlyBudgetDao = monthlyBudgetDao,
            transactionRepository = transactionRepository
        )
    }

    // ViewModels
    val sharedMonthViewModel by lazy {
        SharedMonthViewModel(monthlyBudgetRepository)
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        setupInitialData()
    }

    private fun setupInitialData() {
        applicationScope.launch {
            // 1. Get the current month *first* using the compatible method
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1 // Calendar month is 0-indexed
            val currentMonth = String.format("%d-%02d", year, month) // Creates "YYYY-MM"

            // 2. Check if categories exist *for this month*
            val existingCategories = categoryRepository.getAllCategories(currentMonth).firstOrNull() // <-- FIXED (was line 58)

            if (existingCategories.isNullOrEmpty()) {
                val initialCategories = listOf(
                    "Rent" to 1595.0,
                    "Utilities" to 55.66,
                    "Credit Payment" to 1000.0,
                    "Internet" to 81.63,
                    "Phone" to 21.09,
                    "Transportation" to 85.0,
                    "Discretionary" to 450.0,
                    "Subscription Services" to 50.0,
                    "Cat Essentials" to 100.0,
                    "Laundry" to 20.0,
                    "Renters Insurance" to 24.0
                )

                // 3. We already have currentMonth, so the old line 74 is gone.

                initialCategories.forEach { (name, budget) ->
                    val category = Category(
                        name = name,
                        monthYear = currentMonth, // This was already correct
                        sortOrder = initialCategories.indexOf(name to budget)
                    )
                    val categoryId = categoryRepository.insertCategory(category)

                    // Set initial budget for current month
                    monthlyBudgetDao.insert(
                        MonthlyBudget(
                            categoryId = categoryId,
                            monthYear = currentMonth,
                            budget = budget
                        )
                    )
                }
            }
        }
    }
}
