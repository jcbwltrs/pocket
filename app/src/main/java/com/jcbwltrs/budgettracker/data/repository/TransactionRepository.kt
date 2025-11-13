package com.jcbwltrs.budgettracker.data.repository

import com.jcbwltrs.budgettracker.data.dao.TransactionDao
import com.jcbwltrs.budgettracker.data.model.Transaction
import com.jcbwltrs.budgettracker.data.model.TransactionWithCategoryName
import kotlinx.coroutines.flow.Flow
import java.util.Date

class TransactionRepository(private val transactionDao: TransactionDao) {
    fun getTransactionsForMonth(monthYear: String): Flow<List<TransactionWithCategoryName>> =
        transactionDao.getTransactionsForMonth(monthYear)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insert(transaction)

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

}