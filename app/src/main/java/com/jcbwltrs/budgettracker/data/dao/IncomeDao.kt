package com.jcbwltrs.budgettracker.data.dao

import androidx.room.*
import com.jcbwltrs.budgettracker.data.model.Income
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income ORDER BY date DESC")
    fun getAllIncome(): Flow<List<Income>>

    @Query("SELECT * FROM income WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getIncomeByDateRange(startDate: Date, endDate: Date): Flow<List<Income>>

    @Query("SELECT SUM(amount) FROM income WHERE date >= :startDate AND date <= :endDate")
    fun getTotalIncomeForPeriod(startDate: Date, endDate: Date): Flow<Double?>

    @Insert
    suspend fun insert(income: Income): Long

    @Update
    suspend fun update(income: Income)

    @Delete
    suspend fun delete(income: Income)
}