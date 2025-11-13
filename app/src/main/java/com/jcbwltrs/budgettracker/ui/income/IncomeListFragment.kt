package com.jcbwltrs.budgettracker.ui.income

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jcbwltrs.budgettracker.data.model.Income
import com.jcbwltrs.budgettracker.databinding.FragmentIncomeListBinding
import com.jcbwltrs.budgettracker.ui.dialog.AddIncomeDialog
import com.jcbwltrs.budgettracker.ui.dialog.EditIncomeDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IncomeListFragment : Fragment() {
    private var _binding: FragmentIncomeListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IncomeViewModel by viewModels()
    private lateinit var incomeAdapter: IncomeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupObservers()
        setupClickListeners()
    }

    private fun setupAdapter() {
        incomeAdapter = IncomeAdapter(
            onItemClick = { income ->
                // Handle click if needed
            },
            onEditClick = { income ->
                showEditIncomeDialog(income)
            },
            onDeleteClick = { income ->
                deleteIncome(income)
            }
        )
        binding.rvIncome.adapter = incomeAdapter
    }

    private fun showEditIncomeDialog(income: Income) {
        EditIncomeDialog(
            income = income,
            onIncomeUpdated = { updatedIncome ->
                viewModel.updateIncome(
                    incomeId = updatedIncome.id,
                    amount = updatedIncome.amount,
                    source = updatedIncome.source,
                    description = updatedIncome.description,
                    date = updatedIncome.date
                )
            },
            onIncomeDeleted = { incomeToDelete ->
                deleteIncome(incomeToDelete)
            }
        ).show(childFragmentManager, EditIncomeDialog.TAG)
    }

    private fun deleteIncome(income: Income) {
        viewModel.deleteIncome(income)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.incomeItems.collectLatest { incomeList ->
                incomeAdapter.submitList(incomeList)
                binding.emptyState.visibility =
                    if (incomeList.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalIncome.collectLatest { total ->
                binding.tvTotalIncome.text = String.format("$%.2f", total)
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddIncome.setOnClickListener {
            showAddIncomeDialog()
        }
    }

    private fun showAddIncomeDialog() {
        AddIncomeDialog { amount, source, description, date ->
            viewModel.addIncome(amount, source, description, date)
        }.show(childFragmentManager, "AddIncomeDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
