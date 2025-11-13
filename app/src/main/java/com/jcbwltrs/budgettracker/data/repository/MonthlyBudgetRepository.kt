package com.jcbwltrs.budgettracker.data.repository

import com.jcbwltrs.budgettracker.data.dao.MonthlyBudgetDao
import com.jcbwltrs.budgettracker.data.model.MonthlyBudget
import com.jcbwltrs.budgettracker.data.model.MonthYear
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MonthlyBudgetRepository(
    private val monthlyBudgetDao: MonthlyBudgetDao
) {
    fun getArchivedMonths(): Flow<List<MonthYear>> {
        return monthlyBudgetDao.getArchivedMonths()
            .map { months -> months.map { MonthYear.parse(it) } }
    }

    suspend fun initializeMonth(monthYear: MonthYear, previousMonth: MonthYear?) {
        if (previousMonth != null) {
            val previousBudgets = monthlyBudgetDao.copyBudgetsFromPreviousMonth(previousMonth.toString())
            val newBudgets = previousBudgets.map { budget ->
                budget.copy(monthYear = monthYear.toString(), isArchived = false)
            }
            monthlyBudgetDao.insertAll(newBudgets)
        }
    }

    suspend fun archiveMonth(monthYear: MonthYear) {
        monthlyBudgetDao.archiveMonth(monthYear.toString())
    }

    suspend fun unarchiveMonth(monthYear: MonthYear) {
        monthlyBudgetDao.unarchiveMonth(monthYear.toString())
    }

    fun getMonthlyBudgets(monthYear: MonthYear): Flow<List<MonthlyBudget>> =
        monthlyBudgetDao.getMonthlyBudgets(monthYear.toString())

    suspend fun getCategoryBudget(categoryId: Long, monthYear: MonthYear): Double {
        return monthlyBudgetDao.getCategoryBudget(categoryId, monthYear.toString())?.budget ?: 0.0
    }

    suspend fun updateBudget(categoryId: Long, monthYear: MonthYear, budget: Double) {
        monthlyBudgetDao.insert(MonthlyBudget(
            categoryId = categoryId,
            monthYear = monthYear.toString(),
            budget = budget
        ))
    }

    suspend fun copyBudgetsToNextMonth(currentMonth: MonthYear) {
        val nextMonth = MonthYear(
            month = if (currentMonth.month == 11) 0 else currentMonth.month + 1,
            year = if (currentMonth.month == 11) currentMonth.year + 1 else currentMonth.year
        )
        val currentBudgets = monthlyBudgetDao.getMonthlyBudgetsSync(currentMonth.toString())
        
        val nextMonthBudgets = currentBudgets.map { budget ->
            budget.copy(monthYear = nextMonth.toString())
        }
        
        monthlyBudgetDao.insertAll(nextMonthBudgets)
    }
}
