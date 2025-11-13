package com.jcbwltrs.budgettracker.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jcbwltrs.budgettracker.data.model.MonthYear
import com.jcbwltrs.budgettracker.databinding.DialogMonthSelectorBinding
import com.jcbwltrs.budgettracker.ui.common.MonthAdapter

class MonthSelectorDialog(
    private val currentSelection: MonthYear,
    private val archivedMonths: Set<MonthYear>,
    private val onMonthSelected: (MonthYear) -> Unit,
    private val onMonthArchived: (MonthYear) -> Unit,
    private val onMonthUnarchived: (MonthYear) -> Unit
) : DialogFragment() {

    private var _binding: DialogMonthSelectorBinding? = null
    private val binding get() = _binding!!
    private var showingArchived = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogMonthSelectorBinding.inflate(LayoutInflater.from(context))

        setupMonthsList()
        setupButtons()
        setupHeader()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupHeader() {
        binding.tvTitle.text = if (showingArchived) "Archived Months" else "Select Month"
        binding.tvArchiveNote.isVisible = showingArchived
    }

    private fun setupMonthsList() {
        binding.rvMonths.apply {
            layoutManager = LinearLayoutManager(context)
            updateMonthsList()
        }
    }

    private fun updateMonthsList() {
        binding.rvMonths.adapter = MonthAdapter(
            currentSelection = currentSelection,
            archivedMonths = archivedMonths,
            showArchived = showingArchived,
            onMonthSelected = { selectedMonth ->
                if (!showingArchived) {
                    onMonthSelected(selectedMonth)
                    dismiss()
                }
            },
            onArchiveClicked = { month ->
                if (month.isArchivable()) {
                    if (archivedMonths.contains(month)) {
                        onMonthUnarchived(month)
                    } else {
                        onMonthArchived(month)
                    }
                    updateMonthsList()
                }
            }
        )
    }

    private fun setupButtons() {
        binding.btnArchiveToggle.apply {
            setOnClickListener {
                showingArchived = !showingArchived
                text = if (showingArchived) "Show Active" else "Show Archived"
                setupHeader()
                updateMonthsList()
            }
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            setLayout(width, -2)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MonthSelectorDialog"
    }
}