package com.jcbwltrs.budgettracker.data.repository

import com.jcbwltrs.budgettracker.data.dao.IncomeDao
import com.jcbwltrs.budgettracker.data.model.Income
import kotlinx.coroutines.flow.Flow
import java.util.Date

class IncomeRepository(private val incomeDao: IncomeDao) {
    fun getAllIncome(): Flow<List<Income>> = incomeDao.getAllIncome()

    fun getIncomeByDateRange(startDate: Date, endDate: Date): Flow<List<Income>> =
        incomeDao.getIncomeByDateRange(startDate, endDate)

    fun getTotalIncomeForPeriod(startDate: Date, endDate: Date): Flow<Double?> =
        incomeDao.getTotalIncomeForPeriod(startDate, endDate)

    suspend fun insertIncome(income: Income): Long = incomeDao.insert(income)

    suspend fun updateIncome(income: Income) = incomeDao.update(income)

    suspend fun deleteIncome(income: Income) = incomeDao.delete(income)
}