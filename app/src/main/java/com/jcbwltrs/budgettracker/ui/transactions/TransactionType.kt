package com.jcbwltrs.budgettracker.ui.transactions

enum class TransactionType(val categoryId: Long?) {
    EXPENSE(0),
    INCOME(null) // Income transactions have no category
}