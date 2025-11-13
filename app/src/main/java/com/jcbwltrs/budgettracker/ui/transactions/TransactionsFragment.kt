package com.jcbwltrs.budgettracker.ui.transactions

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jcbwltrs.budgettracker.databinding.FragmentTransactionsBinding
import com.jcbwltrs.budgettracker.ui.SharedMonthViewModel
import com.jcbwltrs.budgettracker.ui.ViewModelFactory
import com.jcbwltrs.budgettracker.ui.dialog.EditTransactionDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val sharedMonthViewModel: SharedMonthViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application)
    }

    private val viewModel: TransactionsViewModel by viewModels {
        ViewModelFactory(requireActivity().application)
    }
    private lateinit var transactionAdapter: TransactionAdapter
    private var toggleAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("Successful push to GitHUb!") // <-- ADD THIS LINE
        setupAdapter()
        setupToggleButtons()
        observeTransactions()

        // Add logging
        viewLifecycleOwner.lifecycleScope.launch {
            sharedMonthViewModel.selectedMonth.collect { month ->
                println("TransactionsFragment: Month changed to ${month.toDisplayString()}")
                viewModel.setSelectedMonth(month)
            }
        }
    }

    private fun setupAdapter() {
        transactionAdapter = TransactionAdapter { transaction ->
            viewLifecycleOwner.lifecycleScope.launch {
                // Check if it's an expense or income
                if (transaction.categoryId != null) {
                    // It's an EXPENSE, get the category
                    val category = viewModel.getCategory(transaction.categoryId)
                    category?.let {
                        EditTransactionDialog(
                            transaction = transaction,
                            category = it, // Pass the category
                            onTransactionUpdated = { updatedTransaction ->
                                viewModel.updateTransaction(updatedTransaction)
                            },
                            onTransactionDeleted = { deletedTransaction ->
                                viewModel.deleteTransaction(deletedTransaction)
                            }
                        ).show(childFragmentManager, EditTransactionDialog.TAG)
                    }
                } else {
                    // It's an INCOME, pass null for the category
                    EditTransactionDialog(
                        transaction = transaction,
                        category = null, // Pass null
                        onTransactionUpdated = { updatedTransaction ->
                            viewModel.updateTransaction(updatedTransaction)
                        },
                        onTransactionDeleted = { deletedTransaction ->
                            viewModel.deleteTransaction(deletedTransaction)
                        }
                    ).show(childFragmentManager, EditTransactionDialog.TAG)
                }
            }
        }
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun setupToggleButtons() {
        // Set initial pill width
        binding.toggleContainer.post {
            val containerWidth = binding.toggleContainer.width - (binding.toggleContainer.paddingLeft + binding.toggleContainer.paddingRight)
            binding.togglePill.layoutParams.width = containerWidth / 2
            binding.togglePill.requestLayout()
        }

        var isExpensesSelected = true

        binding.btnExpenses.setOnClickListener {
            if (!isExpensesSelected) {
                isExpensesSelected = true
                animateToggle(true)
                viewModel.setTransactionType(TransactionType.EXPENSE)
                // observeTransactions() <-- THIS LINE WAS REMOVED
            }
        }

        binding.btnIncome.setOnClickListener {
            if (isExpensesSelected) {
                isExpensesSelected = false
                animateToggle(false)
                viewModel.setTransactionType(TransactionType.INCOME)
            }
        }

        // Set initial states
        binding.btnExpenses.setTextColor(Color.WHITE)
        binding.btnIncome.setTextColor(Color.WHITE.withAlpha(0.6f))
        binding.togglePill.translationX = 0f

        // ADD THIS LINE:
        // Explicitly set the ViewModel state to match the UI
        viewModel.setTransactionType(TransactionType.EXPENSE)
    }

    private fun animateToggle(toExpenses: Boolean) {
        toggleAnimator?.cancel()

        val containerWidth = binding.toggleContainer.width - (binding.toggleContainer.paddingLeft + binding.toggleContainer.paddingRight)
        val distance = containerWidth / 2f

        toggleAnimator = ObjectAnimator.ofFloat(
            binding.togglePill,
            View.TRANSLATION_X,
            if (toExpenses) distance else 0f,
            if (toExpenses) 0f else distance
        ).apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Update text colors
        binding.btnExpenses.setTextColor(if (toExpenses) Color.WHITE else Color.WHITE.withAlpha(0.6f))
        binding.btnIncome.setTextColor(if (!toExpenses) Color.WHITE else Color.WHITE.withAlpha(0.6f))
    }

    private fun observeTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transactions.collectLatest { transactions ->
                transactionAdapter.submitList(transactions)
                binding.tvEmptyState.visibility =
                    if (transactions.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun Int.withAlpha(alpha: Float): Int {
        return Color.argb(
            (alpha * 255).toInt(),
            Color.red(this),
            Color.green(this),
            Color.blue(this)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toggleAnimator?.cancel()
        toggleAnimator = null
        _binding = null
    }
}