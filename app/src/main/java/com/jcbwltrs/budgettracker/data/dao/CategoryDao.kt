package com.jcbwltrs.budgettracker.data.dao

import androidx.room.*
import com.jcbwltrs.budgettracker.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE monthYear = :monthYear ORDER BY sortOrder ASC")
    fun getAllCategories(monthYear: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE monthYear = :monthYear AND isCompleted = 0 ORDER BY sortOrder ASC")
    fun getActiveCategories(monthYear: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE monthYear = :monthYear AND isCompleted = 1 ORDER BY sortOrder ASC")
    fun getCompletedCategories(monthYear: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id AND monthYear = :monthYear")
    suspend fun getCategoryById(id: Long, monthYear: String): Category?

    @Query("INSERT INTO categories (id, monthYear, name, isCompleted, sortOrder) " +
           "SELECT id, :newMonthYear, name, isCompleted, sortOrder " +
           "FROM categories WHERE monthYear = :oldMonthYear")
    suspend fun copyCategoriesToNewMonth(oldMonthYear: String, newMonthYear: String)

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}
