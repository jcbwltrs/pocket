package com.jcbwltrs.budgettracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long?, // Make nullable for income transactions
    val amount: Double,
    val merchant: String,
    val description: String,
    val date: Date,
    val monthYear: String, // Format: "YYYY-MM"
    val createdAt: Date = Date()
)