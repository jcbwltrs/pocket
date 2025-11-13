package com.jcbwltrs.budgettracker.ui.dialog

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jcbwltrs.budgettracker.data.model.Income
import com.jcbwltrs.budgettracker.databinding.DialogEditTransactionBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditIncomeDialog(
    private val income: Income,
    private val onIncomeUpdated: (Income) -> Unit,
    private val onIncomeDeleted: (Income) -> Unit
) : DialogFragment() {

    private var _binding: DialogEditTransactionBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        calendar.time = income.date
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditTransactionBinding.inflate(LayoutInflater.from(context))

        setupInitialValues()
        setupDatePicker()
        setupButtons()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupInitialValues() {
        binding.apply {
            tvCategoryName.text = "Income" // Use this field to show it's an income entry
            etAmount.setText(income.amount.toString())
            etMerchant.setText(income.source)
            etDescription.setText(income.description)
            etDate.setText(dateFormat.format(income.date))
        }
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    binding.etDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupButtons() {
        binding.btnDelete.setOnClickListener {
            onIncomeDeleted(income)
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            val amount = binding.etAmount.text.toString().toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedIncome = income.copy(
                amount = amount,
                source = binding.etMerchant.text.toString(),
                description = binding.etDescription.text.toString(),
                date = calendar.time
            )

            onIncomeUpdated(updatedIncome)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditIncomeDialog"
    }
}
