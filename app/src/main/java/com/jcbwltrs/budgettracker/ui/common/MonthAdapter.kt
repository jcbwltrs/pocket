package com.jcbwltrs.budgettracker.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jcbwltrs.budgettracker.R
import com.jcbwltrs.budgettracker.data.model.MonthYear
import com.jcbwltrs.budgettracker.databinding.ItemMonthBinding
import java.util.Calendar

class MonthAdapter(
    private val currentSelection: MonthYear,
    private val archivedMonths: Set<MonthYear>,
    private val showArchived: Boolean,
    private val onMonthSelected: (MonthYear) -> Unit,
    private val onArchiveClicked: (MonthYear) -> Unit
) : RecyclerView.Adapter<MonthAdapter.MonthViewHolder>() {

    private val months = mutableListOf<MonthYear>()

    init {
        // Get current month and remaining months of current year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Add current month and previous months of current year
        calendar.set(currentYear, Calendar.JANUARY, 1)
        while (calendar.get(Calendar.YEAR) == currentYear) {
            months.add(
                MonthYear(
                    month = calendar.get(Calendar.MONTH),
                    year = calendar.get(Calendar.YEAR)
                )
            )
            calendar.add(Calendar.MONTH, 1)
        }

        // If we're in December, add next year's months
        if (currentMonth == Calendar.DECEMBER) {
            calendar.set(currentYear + 1, Calendar.JANUARY, 1)
            repeat(12) {
                months.add(
                    MonthYear(
                        month = calendar.get(Calendar.MONTH),
                        year = calendar.get(Calendar.YEAR)
                    )
                )
                calendar.add(Calendar.MONTH, 1)
            }
        }
    }

    private val filteredMonths = months.filter { month ->
        val isArchived = archivedMonths.contains(month)
        if (showArchived) isArchived else !isArchived
    }

    inner class MonthViewHolder(
        private val binding: ItemMonthBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(monthYear: MonthYear) {
            with(binding) {
                tvMonth.text = monthYear.toDisplayString()
                root.isSelected = monthYear == currentSelection
                root.alpha = if (monthYear.isFutureMonth()) 0.5f else 1f

                // Show archive button for eligible months
                btnArchive.isVisible = monthYear.isArchivable()
                btnArchive.text = if (archivedMonths.contains(monthYear)) "Unarchive" else "Archive"

                btnArchive.setOnClickListener {
                    onArchiveClicked(monthYear)
                }

                root.setOnClickListener {
                    if (!showArchived) {
                        onMonthSelected(monthYear)
                    }
                }

                // Highlight the current selection
                if (monthYear == currentSelection) {
                    root.setBackgroundColor(ContextCompat.getColor(root.context, R.color.accent_pink_selection))
                } else {
                    root.background = ContextCompat.getDrawable(
                        root.context,
                        R.drawable.ripple_transparent
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        return MonthViewHolder(
            ItemMonthBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        holder.bind(filteredMonths[position])
    }

    override fun getItemCount() = filteredMonths.size
}