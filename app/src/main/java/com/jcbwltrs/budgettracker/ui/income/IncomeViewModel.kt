package com.jcbwltrs.budgettracker.ui.income

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jcbwltrs.budgettracker.BudgetApplication
import com.jcbwltrs.budgettracker.data.model.Income
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

class IncomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<BudgetApplication>()
    private val incomeRepository = app.incomeRepository
    private val transactionRepository = app.transactionRepository
    private val userPreferences = app.userPreferences

    private val _incomeItems = MutableStateFlow<List<Income>>(emptyList())
    val incomeItems = _incomeItems.asStateFlow()

    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome = _totalIncome.asStateFlow()

    private val _averageIncome = MutableStateFlow(0.0)
    val averageIncome = _averageIncome.asStateFlow()

    private val _incomeBySource = MutableStateFlow<Map<String, Double>>(emptyMap())
    val incomeBySource = _incomeBySource.asStateFlow()

    private val _monthlyTrend = MutableStateFlow<List<MonthlyIncomeSummary>>(emptyList())
    val monthlyTrend = _monthlyTrend.asStateFlow()

    private val _selectedMonth = MutableStateFlow(getCurrentMonth())
    val selectedMonth = _selectedMonth.asStateFlow()

    init {
        loadIncomeData()
        calculateMonthlyTrend()
    }

    private fun loadIncomeData() {
        viewModelScope.launch {
            incomeRepository.getAllIncome().collect { incomeList ->
                val filteredList = incomeList.filter { income ->
                    val calendar = Calendar.getInstance().apply { time = income.date }
                    calendar.get(Calendar.MONTH) == _selectedMonth.value
                }

                // Update income items sorted by date
                _incomeItems.value = filteredList.sortedByDescending { it.date }

                // Calculate total income
                _totalIncome.value = filteredList.sumOf { it.amount }

                // Calculate average income per entry
                _averageIncome.value = if (filteredList.isNotEmpty()) {
                    filteredList.sumOf { it.amount } / filteredList.size
                } else 0.0

                // Group income by source
                _incomeBySource.value = filteredList.groupBy { it.source }
                    .mapValues { (_, incomes) -> incomes.sumOf { it.amount } }
                    .toSortedMap()
            }
        }
    }

    fun setSelectedMonth(month: Int) {
        _selectedMonth.value = month
        loadIncomeData()
    }

    private fun getCurrentMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH)
    }

    private fun calculateMonthlyTrend() {
        viewModelScope.launch {
            val lastSixMonths = mutableListOf<MonthlyIncomeSummary>()
            val calendar = Calendar.getInstance()

            incomeRepository.getAllIncome().collect { allIncome ->
                repeat(6) { monthsAgo ->
                    calendar.time = Date()
                    calendar.add(Calendar.MONTH, -monthsAgo)

                    val monthStart = calendar.apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.time

                    val monthEnd = calendar.apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }.time

                    val monthlyIncome = allIncome.filter { income ->
                        income.date in monthStart..monthEnd
                    }

                    val total = monthlyIncome.sumOf { it.amount }
                    val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
                    lastSixMonths.add(MonthlyIncomeSummary(monthName ?: "", total))
                }
                _monthlyTrend.value = lastSixMonths.reversed()
            }
        }
    }

    fun getIncomeStats(): IncomeStatistics {
        val currentList = _incomeItems.value
        return IncomeStatistics(
            totalIncome = currentList.sumOf { it.amount },
            averageIncome = _averageIncome.value,
            numberOfEntries = currentList.size,
            highestIncome = currentList.maxByOrNull { it.amount }?.amount ?: 0.0,
            lowestIncome = currentList.minByOrNull { it.amount }?.amount ?: 0.0,
            mostFrequentSource = currentList.groupBy { it.source }
                .maxByOrNull { it.value.size }?.key ?: "",
            mostValuableSource = _incomeBySource.value
                .maxByOrNull { it.value }?.key ?: ""
        )
    }

    fun addIncome(amount: Double, source: String, description: String, date: Date) {
        viewModelScope.launch {
            val income = Income(
                amount = amount,
                source = source,
                description = description,
                date = date
            )
            // 1. Insert into the (legacy) Income table
            incomeRepository.insertIncome(income)

            // 2. Insert into the Transaction table (the new source of truth)
            val transaction = com.jcbwltrs.budgettracker.data.model.Transaction(
                categoryId = null,  // Income transactions have no category
                amount = amount,
                merchant = source,
                description = description,
                date = date,
                monthYear = getCurrentMonthYear() // Make sure this function exists
            )
            transactionRepository.insertTransaction(transaction)
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            // Delete both income and its corresponding transaction
            incomeRepository.deleteIncome(income)

            // Find and delete the corresponding transaction
            val transactions = transactionRepository.getTransactionsForMonth(getCurrentMonthYear()).first()
            transactions.find {
                it.transaction.categoryId == null &&
                        it.transaction.amount == income.amount &&
                        it.transaction.date == income.date
                // This lookup is fragile, but it's what you have. A shared ID would be better.
            }?.let { matchingTransaction ->
                transactionRepository.deleteTransaction(matchingTransaction.transaction)
            }
        }
    }

    fun updateIncome(
        incomeId: Long,
        amount: Double,
        source: String,
        description: String,
        date: Date
    ) {
        viewModelScope.launch {
            val updatedIncome = Income(
                id = incomeId,
                amount = amount,
                source = source,
                description = description,
                date = date
            )
            // We need to find the *old* income to find its matching transaction
            val oldIncome = incomeRepository.getAllIncome().first().find { it.id == incomeId } ?: return@launch

            incomeRepository.updateIncome(updatedIncome)

            // Update corresponding transaction
            val transactions = transactionRepository.getTransactionsForMonth(getCurrentMonthYear()).first()
            transactions.find {
                it.transaction.categoryId == null &&
                        it.transaction.merchant == oldIncome.source && // Use oldIncome for lookup
                        it.transaction.date == oldIncome.date         // Use oldIncome for lookup
            }?.let { matchingTransaction ->
                val updatedTransaction = matchingTransaction.transaction.copy(
                    amount = amount,
                    merchant = source,
                    description = description,
                    date = date
                )
                transactionRepository.updateTransaction(updatedTransaction)
            }
        }
    }

    private fun getCurrentMonthYear(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        return String.format("%04d-%02d", year, month)
    }

    data class MonthlyIncomeSummary(
        val month: String,
        val total: Double
    )

    data class IncomeStatistics(
        val totalIncome: Double,
        val averageIncome: Double,
        val numberOfEntries: Int,
        val highestIncome: Double,
        val lowestIncome: Double,
        val mostFrequentSource: String,
        val mostValuableSource: String
    )
}
