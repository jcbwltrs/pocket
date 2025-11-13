package com.jcbwltrs.budgettracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "monthly_budgets",
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
data class MonthlyBudget(
    val categoryId: Long,
    val monthYear: String, // Format: "YYYY-MM"
    val budget: Double = 0.0,
    val isArchived: Boolean = false
)
