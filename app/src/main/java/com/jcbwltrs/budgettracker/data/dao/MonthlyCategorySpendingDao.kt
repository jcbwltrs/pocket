package com.jcbwltrs.budgettracker.data.dao

import androidx.room.*
import com.jcbwltrs.budgettracker.data.model.MonthlyCategorySpending
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyCategorySpendingDao {
    @Query("SELECT * FROM monthly_category_spending WHERE monthYear = :monthYear")
    fun getMonthlySpendings(monthYear: String): Flow<List<MonthlyCategorySpending>>

    @Query("SELECT * FROM monthly_category_spending WHERE categoryId = :categoryId AND monthYear = :monthYear")
    suspend fun getCategorySpending(categoryId: Long, monthYear: String): MonthlyCategorySpending?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spending: MonthlyCategorySpending)

    @Update
    suspend fun update(spending: MonthlyCategorySpending)
}