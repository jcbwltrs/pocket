package com.jcbwltrs.budgettracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories" // <-- REMOVE THE primaryKeys LINE
)
data class Category(
    @PrimaryKey(autoGenerate = true) // <-- ADD THIS ANNOTATION
    val id: Long = 0,
    val monthYear: String, // Format: "YYYY-MM"
    val name: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)
