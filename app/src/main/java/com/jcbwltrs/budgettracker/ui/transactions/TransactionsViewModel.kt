package com.jcbwltrs.budgettracker.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jcbwltrs.budgettracker.BudgetApplication
import com.jcbwltrs.budgettracker.data.model.Category
import com.jcbwltrs.budgettracker.data.model.MonthYear
import com.jcbwltrs.budgettracker.data.model.Transaction
import com.jcbwltrs.budgettracker.data.model.TransactionWithCategoryName
import com.jcbwltrs.budgettracker.ui.SharedMonthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
@kotlinx.coroutines.ExperimentalCoroutinesApi
class TransactionsViewModel(
    application: Application,
    private val sharedMonthViewModel: SharedMonthViewModel
) : AndroidViewModel(application) {
    private val app = getApplication<BudgetApplication>()
    private val transactionRepository = app.transactionRepository
    private val categoryRepository = app.categoryRepository
    private val incomeRepository = app.incomeRepository

    private val _selectedMonth = MutableStateFlow(MonthYear.current())
    private val _selectedTransactionType = MutableStateFlow(TransactionType.EXPENSE)
    private val _transactions = MutableStateFlow<List<TransactionWithCategoryName>>(emptyList())
    val transactions = _transactions.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _selectedMonth,
                _selectedTransactionType
            ) { month, type -> // <-- I removed the explicit types to help the compiler
                Pair(month, type)
            }.flatMapLatest { (month, type) ->
                transactionRepository.getTransactionsForMonth(month.toString())
                    .map { allTransactions ->
                        when (type) {
                            TransactionType.EXPENSE -> {
                                allTransactions
                                    .filter { it.transaction.categoryId != null }
                                    .sortedByDescending { it.transaction.date }
                            }
                            TransactionType.INCOME -> {
                                allTransactions
                                    .filter { it.transaction.categoryId == null }
                                    .sortedByDescending { it.transaction.date }
                            }
                            // <-- The 'else' branch is removed, as the warning said it was redundant
                        }
                    }
            }.collect { filteredTransactions ->
                _transactions.value = filteredTransactions
            }
        }
    }

    private fun MonthYear.toStartDate(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun MonthYear.toEndDate(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }

    fun setSelectedMonth(month: MonthYear) {
        _selectedMonth.value = month
    }

    fun setTransactionType(type: TransactionType) {
        _selectedTransactionType.value = type
    }

    suspend fun getCategory(categoryId: Long?): Category? {
        val currentMonth = _selectedMonth.value.toString() // Get the current month
        return categoryId?.let { categoryRepository.getCategoryById(it, currentMonth) }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
            transaction.categoryId?.let {
                categoryRepository.updateSpentAmount(
                    it,
                    transaction.monthYear
                )
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
            transaction.categoryId?.let {
                categoryRepository.updateSpentAmount(
                    it,
                    transaction.monthYear
                )
            }
        }
    }
}