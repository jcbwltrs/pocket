package com.jcbwltrs.budgettracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcbwltrs.budgettracker.data.model.MonthYear
import com.jcbwltrs.budgettracker.data.repository.MonthlyBudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SharedMonthViewModel(
    private val monthlyBudgetRepository: MonthlyBudgetRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(MonthYear.current())
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _archivedMonths = MutableStateFlow<Set<MonthYear>>(emptySet())
    val archivedMonths = _archivedMonths.asStateFlow()

    init {
        // Load archived months
        viewModelScope.launch {
            monthlyBudgetRepository.getArchivedMonths()
                .collect { months ->
                    _archivedMonths.value = months.toSet()
                }
        }
    }

    fun selectMonth(monthYear: MonthYear) {
        viewModelScope.launch {

            println("Selecting month: ${monthYear.toDisplayString()}")

            if (!_archivedMonths.value.contains(monthYear)) {

                monthlyBudgetRepository.initializeMonth(
                    monthYear = monthYear,
                    previousMonth = _selectedMonth.value
                )
            }
            _selectedMonth.value = monthYear
        }
    }

    fun archiveMonth(monthYear: MonthYear) {
        viewModelScope.launch {
            if (monthYear.isArchivable()) {
                monthlyBudgetRepository.archiveMonth(monthYear)
                // If archived month is selected, switch to current month
                if (_selectedMonth.value == monthYear) {
                    selectMonth(MonthYear.current())
                }
            }
        }
    }

    fun unarchiveMonth(monthYear: MonthYear) {
        viewModelScope.launch {
            monthlyBudgetRepository.unarchiveMonth(monthYear)
        }
    }

    fun isMonthArchived(monthYear: MonthYear): Boolean {
        return _archivedMonths.value.contains(monthYear)
    }
}