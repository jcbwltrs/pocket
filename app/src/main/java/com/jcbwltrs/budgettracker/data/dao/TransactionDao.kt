package com.jcbwltrs.budgettracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction as RoomTransaction
import com.jcbwltrs.budgettracker.data.model.Transaction as ModelTransaction
import com.jcbwltrs.budgettracker.data.model.TransactionWithCategoryName
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TransactionDao {

    @RoomTransaction
    @Query("""
        SELECT t.*, COALESCE(c.name, 'Income') as categoryName 
        FROM transactions t 
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.monthYear = :monthYear
    """)
    fun getTransactionsForMonth(monthYear: String): Flow<List<TransactionWithCategoryName>>

    @Insert
    suspend fun insert(transaction: ModelTransaction): Long

    @Update
    suspend fun update(transaction: ModelTransaction)

    @Delete
    suspend fun delete(transaction: ModelTransaction)

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<ModelTransaction>>
}
