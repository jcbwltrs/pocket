package com.jcbwltrs.budgettracker.data.model

import androidx.room.Embedded
import androidx.room.ColumnInfo
import java.util.Date

data class TransactionWithCategoryName(
    @Embedded val transaction: Transaction,
    @ColumnInfo(name = "categoryName") val categoryName: String
) {
    // Add a no-arg constructor for Room
    constructor() : this(
        transaction = Transaction(
            categoryId = null,  // Changed to null to match nullable type
            amount = 0.0,
            merchant = "",
            description = "",
            date = Date(),
            monthYear = ""
        ),
        categoryName = ""
    )
}