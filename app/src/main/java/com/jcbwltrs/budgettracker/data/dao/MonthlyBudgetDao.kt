package com.jcbwltrs.budgettracker.data.dao

import androidx.room.*
import com.jcbwltrs.budgettracker.data.model.MonthlyBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE monthYear = :monthYear")
    fun getMonthlyBudgets(monthYear: String): Flow<List<MonthlyBudget>>

    @Query("SELECT * FROM monthly_budgets WHERE categoryId = :categoryId AND monthYear = :monthYear")
    suspend fun getCategoryBudget(categoryId: Long, monthYear: String): MonthlyBudget?

    @Query("SELECT * FROM monthly_budgets WHERE monthYear = :monthYear ORDER BY categoryId")
    suspend fun getMonthlyBudgetsSync(monthYear: String): List<MonthlyBudget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: MonthlyBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<MonthlyBudget>)

    @Query("SELECT * FROM monthly_budgets WHERE monthYear = :previousMonth")
    suspend fun copyBudgetsFromPreviousMonth(previousMonth: String): List<MonthlyBudget>

    @Query("SELECT DISTINCT monthYear FROM monthly_budgets WHERE isArchived = 1")
    fun getArchivedMonths(): Flow<List<String>>

    @Query("UPDATE monthly_budgets SET isArchived = 1 WHERE monthYear = :monthYear")
    suspend fun archiveMonth(monthYear: String)

    @Query("UPDATE monthly_budgets SET isArchived = 0 WHERE monthYear = :monthYear")
    suspend fun unarchiveMonth(monthYear: String)
}
