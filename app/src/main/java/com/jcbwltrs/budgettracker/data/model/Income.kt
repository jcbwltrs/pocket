package com.jcbwltrs.budgettracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "income")
data class Income(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val source: String,
    val description: String,
    val date: Date,
    val createdAt: Date = Date()
)