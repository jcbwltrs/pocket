package com.jcbwltrs.budgettracker.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jcbwltrs.budgettracker.databinding.DialogConfigureBalanceBinding
import com.jcbwltrs.budgettracker.ui.ViewModelFactory
import com.jcbwltrs.budgettracker.ui.dashboard.DashboardViewModel

class ConfigureBalanceDialog : DialogFragment() {
    private var _binding: DialogConfigureBalanceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogConfigureBalanceBinding.inflate(layoutInflater)

        // Set current balance in EditText if available
        viewModel.startingBalance.value.let { balance ->
            binding.editTextBalance.setText(String.format("%.2f", balance))
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Configure Starting Balance")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val amount = binding.editTextBalance.text.toString().toDoubleOrNull() ?: 0.0
                viewModel.updateStartingBalance(amount)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ConfigureBalanceDialog"
    }
}