package com.jcbwltrs.budgettracker.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jcbwltrs.budgettracker.BudgetApplication
import com.jcbwltrs.budgettracker.ui.dashboard.DashboardViewModel
import com.jcbwltrs.budgettracker.ui.transactions.TransactionsViewModel

class ViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = application as BudgetApplication
        return when {
            modelClass.isAssignableFrom(SharedMonthViewModel::class.java) -> {
                app.sharedMonthViewModel as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    application = application,
                    sharedMonthViewModel = app.sharedMonthViewModel
                ) as T
            }
            modelClass.isAssignableFrom(TransactionsViewModel::class.java) -> {
                TransactionsViewModel(
                    application = application,
                    sharedMonthViewModel = app.sharedMonthViewModel
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}