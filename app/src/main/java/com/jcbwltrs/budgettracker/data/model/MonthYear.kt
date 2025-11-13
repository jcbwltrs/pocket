package com.jcbwltrs.budgettracker.data.model

import java.util.Calendar

data class MonthYear(
    val month: Int,  // 0-11 for Jan-Dec
    val year: Int
) {
    companion object {
        fun current(): MonthYear {
            val calendar = Calendar.getInstance()
            return MonthYear(
                month = calendar.get(Calendar.MONTH),
                year = calendar.get(Calendar.YEAR)
            )
        }

        fun parse(monthYear: String): MonthYear {
            // Format: "YYYY-MM"
            val parts = monthYear.split("-")
            if (parts.size != 2) throw IllegalArgumentException("Invalid month-year format: $monthYear")
            
            val year = parts[0].toIntOrNull() 
                ?: throw IllegalArgumentException("Invalid year: ${parts[0]}")
            val month = parts[1].toIntOrNull()?.minus(1) // Convert 1-12 to 0-11
                ?: throw IllegalArgumentException("Invalid month: ${parts[1]}")
                
            if (month !in 0..11) throw IllegalArgumentException("Month must be between 1 and 12")
            
            return MonthYear(month = month, year = year)
        }
    }

    override fun toString(): String {
        return "$year-${month + 1}" // Convert 0-11 back to 1-12
    }

    fun toDisplayString(): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
        }
        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.getDefault())
        return "$monthName $year"
    }

    fun isCurrentMonth(): Boolean {
        val current = current()
        return this.month == current.month && this.year == current.year
    }

    fun isFutureMonth(): Boolean {
        val current = current()
        return when {
            this.year > current.year -> true
            this.year < current.year -> false
            else -> this.month > current.month
        }
    }

    fun isArchivable(): Boolean {
        val current = current()
        return when {
            this.year < current.year -> true
            this.year > current.year -> false
            else -> this.month < current.month
        }
    }
}
