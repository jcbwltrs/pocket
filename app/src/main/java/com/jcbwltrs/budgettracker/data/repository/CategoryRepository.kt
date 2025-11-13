package com.jcbwltrs.budgettracker.data.repository

import com.jcbwltrs.budgettracker.data.dao.CategoryDao
import com.jcbwltrs.budgettracker.data.dao.MonthlyBudgetDao
import com.jcbwltrs.budgettracker.data.dao.MonthlyCategorySpendingDao
import com.jcbwltrs.budgettracker.data.model.Category
import com.jcbwltrs.budgettracker.data.model.MonthlyBudget
import com.jcbwltrs.budgettracker.data.model.MonthlyCategorySpending
import com.jcbwltrs.budgettracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val monthlyCategorySpendingDao: MonthlyCategorySpendingDao,
    private val monthlyBudgetDao: MonthlyBudgetDao,
    private val transactionRepository: TransactionRepository
) {
    fun getAllCategories(monthYear: String): Flow<List<Category>> = categoryDao.getAllCategories(monthYear)

    fun getCategoriesWithMonthlyData(monthYear: String): Flow<List<Triple<Category, Double, Double>>> {
        return combine(
            getAllCategories(monthYear),
            monthlyCategorySpendingDao.getMonthlySpendings(monthYear),
            monthlyBudgetDao.getMonthlyBudgets(monthYear)
        ) { categories, spendings, budgets ->
            categories.map { category ->
                val monthlySpent = spendings.find { it.categoryId == category.id }?.spent ?: 0.0
                val monthlyBudget = budgets.find { it.categoryId == category.id }?.budget ?: 0.0
                Triple(category, monthlySpent, monthlyBudget)
            }
        }
    }

    suspend fun getCategoryById(id: Long, monthYear: String): Category? = categoryDao.getCategoryById(id, monthYear)

    suspend fun insertCategory(category: Category): Long = categoryDao.insert(category)

    suspend fun updateCategory(category: Category) = categoryDao.update(category)

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun updateSpentAmount(categoryId: Long, monthYear: String, previousMonthYear: String? = null) {
        val transactions = transactionRepository.getTransactionsForMonth(monthYear).first() // <-- FIXED
        val categoryTransactions = transactions.filter {
            it.transaction.categoryId == categoryId
            // No need to filter by monthYear again, the database already did it!
        }
        val totalSpent = categoryTransactions.sumOf { it.transaction.amount }

        // Update current month's spending
        val spending = MonthlyCategorySpending(
            categoryId = categoryId,
            monthYear = monthYear,
            spent = totalSpent
        )
        monthlyCategorySpendingDao.insert(spending)

        // If this is a new month, copy the budget from the previous month
        if (previousMonthYear != null) {
            val previousBudget = monthlyBudgetDao.getCategoryBudget(categoryId, previousMonthYear)
            if (previousBudget != null) {
                monthlyBudgetDao.insert(previousBudget.copy(monthYear = monthYear))
            }
        }
    }

    suspend fun recalculateMonthlySpending(monthYear: String) {
        val categories = getAllCategories(monthYear).first()
        val transactions = transactionRepository.getTransactionsForMonth(monthYear).first() // <-- FIXED

        categories.forEach { category ->
            val categoryTransactions = transactions.filter {
                it.transaction.categoryId == category.id
                // No need to filter by monthYear again!
            }
            val totalSpent = categoryTransactions.sumOf { it.transaction.amount }

            val spending = MonthlyCategorySpending(
                categoryId = category.id,
                monthYear = monthYear,
                spent = totalSpent
            )
            monthlyCategorySpendingDao.insert(spending)
        }
    }
}
