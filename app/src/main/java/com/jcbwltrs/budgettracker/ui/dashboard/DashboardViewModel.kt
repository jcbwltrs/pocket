package com.jcbwltrs.budgettracker.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jcbwltrs.budgettracker.BudgetApplication
import com.jcbwltrs.budgettracker.data.model.*
import com.jcbwltrs.budgettracker.ui.SharedMonthViewModel
import com.jcbwltrs.budgettracker.ui.transactions.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class DashboardViewModel(
    application: Application,
    private val sharedMonthViewModel: SharedMonthViewModel
) : AndroidViewModel(application) {
    private val app = getApplication<BudgetApplication>()
    private val categoryRepository = app.categoryRepository
    private val transactionRepository = app.transactionRepository
    private val incomeRepository = app.incomeRepository
    private val userPreferences = app.userPreferences
    private val monthlyBudgetRepository = app.monthlyBudgetRepository

    private val _selectedMonth = MutableStateFlow(MonthYear.current())
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _activeCategories = MutableStateFlow<List<Triple<Category, Double, Double>>>(emptyList())
    val activeCategories = _activeCategories.asStateFlow()

    private val _completedCategories = MutableStateFlow<List<Triple<Category, Double, Double>>>(emptyList())
    val completedCategories = _completedCategories.asStateFlow()

    private val _totalBudget = MutableStateFlow(0.0)
    val totalBudget = _totalBudget.asStateFlow()

    private val _totalSpent = MutableStateFlow(0.0)
    val totalSpent = _totalSpent.asStateFlow()

    private val _currentBalance = MutableStateFlow(0.0)
    val currentBalance = _currentBalance.asStateFlow()

    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome = _totalIncome.asStateFlow()

    private val _startingBalance = MutableStateFlow(0.0)
    val startingBalance = _startingBalance.asStateFlow()

    private var lastSelectedMonth: MonthYear? = null

    init {
        viewModelScope.launch {
            println("DashboardViewModel initialized")

            sharedMonthViewModel.selectedMonth
                .onEach { selectedMonth ->
                    println("Month changed to: ${selectedMonth.toDisplayString()}")
                    _selectedMonth.value = selectedMonth
                }
                .flatMapLatest { selectedMonth ->
                    combine(
                        userPreferences.startingBalance,
                        categoryRepository.getCategoriesWithMonthlyData(selectedMonth.toString()),
                        transactionRepository.getTransactionsForMonth(selectedMonth.toString()),
                        sharedMonthViewModel.archivedMonths
                    ) { startBalance, categoriesWithData, transactions, archivedMonths ->
                        println("Processing data for month: ${selectedMonth.toDisplayString()}")
                        processMonthlyData(
                            currentMonth = selectedMonth,
                            startBalance = startBalance,
                            categoriesWithData = categoriesWithData,
                            transactions = transactions,
                            archivedMonths = archivedMonths
                        )
                    }
                }
                .collect()
        }
    }

    private fun processMonthlyData(
        currentMonth: MonthYear,
        startBalance: Double,
        categoriesWithData: List<Triple<Category, Double, Double>>,
        transactions: List<TransactionWithCategoryName>,
        archivedMonths: Set<MonthYear>
    ) {
        if (archivedMonths.contains(currentMonth)) {
            clearMonthData()
        } else {
            updateMonthData(
                currentMonth,
                startBalance,
                categoriesWithData,
                transactions
            )
        }
    }

    private fun clearMonthData() {
        _activeCategories.value = emptyList()
        _completedCategories.value = emptyList()
        _totalBudget.value = 0.0
        _totalSpent.value = 0.0
        _currentBalance.value = 0.0
        _totalIncome.value = 0.0
        _startingBalance.value = 0.0
    }

    private fun updateMonthData(
        currentMonth: MonthYear,
        startBalance: Double,
        categoriesWithData: List<Triple<Category, Double, Double>>,
        transactions: List<TransactionWithCategoryName>
    ) {
        _startingBalance.value = startBalance
        _totalBudget.value = categoriesWithData.sumOf { it.third }

        val monthlyTransactions = transactions
            .filter { txn -> txn.transaction.monthYear == currentMonth.toString() }

        println("Found ${monthlyTransactions.size} transactions for month")

        // Separate expenses and income
        val expenseTransactions = monthlyTransactions.filter { it.transaction.categoryId != null }
        val incomeTransactions = monthlyTransactions.filter { it.transaction.categoryId == null }

        // Calculate totals
        val totalSpent = expenseTransactions.sumOf { it.transaction.amount }
        val totalIncome = incomeTransactions.sumOf { it.transaction.amount }

        _totalSpent.value = totalSpent
        _totalIncome.value = totalIncome // Now only uses income from transactions
        _currentBalance.value = startBalance + _totalIncome.value - totalSpent

        val (completed, active) = categoriesWithData.partition { (_, spent, budget) ->
            spent >= budget
        }

        _activeCategories.value = active
            .sortedWith(
                compareByDescending<Triple<Category, Double, Double>> { (_, spent, budget) ->
                    spent > budget
                }.thenBy { it.first.name }
            )

        _completedCategories.value = completed
            .sortedBy { it.first.name }

        // Handle month change and balance rollover
        if (lastSelectedMonth != null && lastSelectedMonth != currentMonth) {
            handleMonthChange(lastSelectedMonth!!, currentMonth)
        }
        lastSelectedMonth = currentMonth
    }

    private fun handleMonthChange(oldMonth: MonthYear, newMonth: MonthYear) {
        viewModelScope.launch {
            // Set previous month's ending balance as new month's starting balance
            userPreferences.saveStartingBalance(_currentBalance.value)

            // Copy budgets to new month
            monthlyBudgetRepository.copyBudgetsToNextMonth(oldMonth)
        }
    }

    private fun getMonthStartDate(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedMonth.value.year)
            set(Calendar.MONTH, selectedMonth.value.month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun getMonthEndDate(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedMonth.value.year)
            set(Calendar.MONTH, selectedMonth.value.month)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }

    fun setSelectedMonth(monthYear: MonthYear) {
        _selectedMonth.value = monthYear
    }

    fun isCurrentMonthArchived(): Boolean {
        return sharedMonthViewModel.isMonthArchived(selectedMonth.value)
    }

    fun addIncome(amount: Double, source: String, description: String, date: Date) {
        viewModelScope.launch {
            val income = Income(
                amount = amount,
                source = source,
                description = description,
                date = date
            )
            incomeRepository.insertIncome(income)

            val transaction = Transaction(
                categoryId = null,  // Income transactions have no category
                amount = amount,    // Keep positive for consistency
                merchant = source,
                description = description,
                date = date,
                monthYear = selectedMonth.value.toString()
            )
            transactionRepository.insertTransaction(transaction)
        }
    }

    fun addTransaction(
        categoryId: Long,
        amount: Double,
        merchant: String,
        description: String,
        date: Date
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                categoryId = categoryId,
                amount = amount,
                merchant = merchant,
                description = description,
                date = date,
                monthYear = selectedMonth.value.toString()
            )
            transactionRepository.insertTransaction(transaction)
            categoryRepository.recalculateMonthlySpending(selectedMonth.value.toString())
        }
    }

    fun updateStartingBalance(newBalance: Double) {
        viewModelScope.launch {
            userPreferences.saveStartingBalance(newBalance)
        }
    }

    fun calculateRemainingDays(): Int {
        val today = Calendar.getInstance()
        val lastDay = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        return lastDay.get(Calendar.DAY_OF_MONTH) - today.get(Calendar.DAY_OF_MONTH) + 1
    }

    fun calculateDailyBudget(category: Category, budget: Double): Double {
        val spent = _activeCategories.value
            .find { it.first.id == category.id }?.second
            ?: _completedCategories.value
                .find { it.first.id == category.id }?.second
            ?: 0.0

        val remaining = budget - spent
        return remaining / calculateRemainingDays()
    }

    fun calculateBusRides(amount: Double): Int {
        return (amount / 2.25).toInt()
    }

    fun createCategory(name: String, budget: Double) {
        viewModelScope.launch {
            val newCategory = Category(
                name = name,
                monthYear = selectedMonth.value.toString(),
                sortOrder = (_activeCategories.value.size + _completedCategories.value.size)
            )
            val categoryId = categoryRepository.insertCategory(newCategory)
            
            // Set budget for current month
            monthlyBudgetRepository.updateBudget(
                categoryId = categoryId,
                monthYear = selectedMonth.value,
                budget = budget
            )
        }
    }

    fun updateCategory(category: Category, newBudget: Double) {
        viewModelScope.launch {
            // Update category for current month only
            val updatedCategory = category.copy(monthYear = selectedMonth.value.toString())
            categoryRepository.updateCategory(updatedCategory)
            monthlyBudgetRepository.updateBudget(
                categoryId = category.id,
                monthYear = selectedMonth.value,
                budget = newBudget
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            // Delete category for current month only
            val categoryToDelete = category.copy(monthYear = selectedMonth.value.toString())
            categoryRepository.deleteCategory(categoryToDelete)
        }
    }
}
