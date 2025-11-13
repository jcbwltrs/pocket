package com.jcbwltrs.budgettracker.ui.dialog

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.jcbwltrs.budgettracker.data.model.Category
import com.jcbwltrs.budgettracker.databinding.DialogEditCategoryBinding
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.Locale
import android.content.DialogInterface

class EditCategoryDialog(
    private val category: Category,
    private val currentBudget: Double,
    private val onCategoryEdited: (String, Double) -> Unit,
    private val onCategoryDeleted: () -> Unit,
    private val isNewCategory: Boolean = false
) : DialogFragment() {

    private var _binding: DialogEditCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInitialValues()
        setupListeners()

        binding.btnDelete.visibility = if (isNewCategory) View.GONE else View.VISIBLE
    }

    private fun setupInitialValues() {
        binding.etCategoryName.setText(category.name)
        // Show initial budget formatted as currency
        binding.etBudget.setText(NumberFormat.getCurrencyInstance(Locale.US).format(currentBudget))
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            val name = binding.etCategoryName.text.toString().trim()
            val budgetStr = binding.etBudget.text.toString().trim()
                .replace("[^\\d.]".toRegex(), "") // Remove all non-numeric characters

            if (name.isBlank()) {
                binding.etCategoryName.error = "Category name is required"
                return@setOnClickListener
            }

            try {
                val budget = budgetStr.toDouble()
                if (budget <= 0) {
                    binding.etBudget.error = "Budget must be greater than 0"
                    return@setOnClickListener
                }
                onCategoryEdited(name, budget)
                dismiss()
            } catch (e: NumberFormatException) {
                binding.etBudget.error = "Invalid budget amount"
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Handle focus changes on the budget field
        binding.etBudget.setOnFocusChangeListener { view, hasFocus ->
            val editText = view as TextInputEditText
            val budgetStr = editText.text.toString()

            if (hasFocus) {
                // When focused, show plain number for editing
                val cleanString = budgetStr.replace("[^\\d.]".toRegex(), "")
                editText.setText(cleanString)
            } else {
                // When focus is lost, format as currency
                try {
                    val parsed = budgetStr.replace("[^\\d.]".toRegex(), "").toDouble()
                    val formatted = NumberFormat.getCurrencyInstance(Locale.US).format(parsed)
                    editText.setText(formatted)
                } catch (e: NumberFormatException) {
                    // If parsing fails, leave as is
                }
            }
        }
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete this category? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog: DialogInterface, _: Int ->
                onCategoryDeleted()
                dialog.dismiss()
                dismiss()
            }
            .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditCategoryDialog"
    }
}
