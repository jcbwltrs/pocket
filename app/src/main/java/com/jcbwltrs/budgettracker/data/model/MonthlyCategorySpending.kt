package com.jcbwltrs.budgettracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "monthly_category_spending",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")],
    primaryKeys = ["categoryId", "monthYear"]
)
data class MonthlyCategorySpending(
    val categoryId: Long,
    val monthYear: String, // Format: "YYYY-MM"
    val spent: Double = 0.0
)