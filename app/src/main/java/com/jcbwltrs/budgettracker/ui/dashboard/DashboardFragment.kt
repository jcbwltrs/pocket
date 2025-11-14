package com.jcbwltrs.budgettracker.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jcbwltrs.budgettracker.R
import com.jcbwltrs.budgettracker.data.model.Category
import com.jcbwltrs.budgettracker.databinding.FragmentDashboardBinding
import com.jcbwltrs.budgettracker.ui.SharedMonthViewModel
import com.jcbwltrs.budgettracker.ui.ViewModelFactory
import com.jcbwltrs.budgettracker.ui.dialog.AddIncomeDialog
import com.jcbwltrs.budgettracker.ui.dialog.AddTransactionDialog
import com.jcbwltrs.budgettracker.ui.dialog.ConfigureBalanceDialog
import com.jcbwltrs.budgettracker.ui.dialog.EditCategoryDialog
import com.jcbwltrs.budgettracker.ui.dialog.MonthSelectorDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val sharedMonthViewModel: SharedMonthViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application)
    }

    private val viewModel: DashboardViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application
        )
    }
    private lateinit var activeAdapter: CategoryAdapter
    private lateinit var completedAdapter: CategoryAdapter
    private var isCompletedExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This ID is still valid, as it's in fragment_dashboard.xml
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        binding.tvCurrentDate.text = dateFormat.format(Date())

        viewLifecycleOwner.lifecycleScope.launch {
            sharedMonthViewModel.selectedMonth.collect { month ->
                println("DashboardFragment: Month changed to ${month.toDisplayString()}")
            }
        }

        setupAdapters()
        setupObservers()
        setupClickListeners() // We'll add the new listeners here

        // REMOVED: binding.fabAddCategory.setOnClickListener
    }

    private fun showCreateCategoryDialog() {
        val currentMonth = viewModel.selectedMonth.value.toString()
        EditCategoryDialog(
            category = Category(name = "", monthYear = currentMonth),
            currentBudget = 0.0,
            onCategoryEdited = { name, budget ->
                viewModel.createCategory(name, budget)
            },
            onCategoryDeleted = { /* Do nothing for new categories */ },
            isNewCategory = true
        ).show(parentFragmentManager, EditCategoryDialog.TAG)
    }

    private fun setupAdapters() {
        activeAdapter = CategoryAdapter(
            onCategoryClick = { categoryData ->
                showAddTransactionDialog(categoryData.first)
            },
            onCategoryLongClick = { categoryData ->
                showEditCategoryDialog(categoryData)
                true  // Return true to indicate the long click was handled
            },
            // FIXED: Add the new listener
            onAddCategoryClick = {
                showCreateCategoryDialog()
            },
            calculateDailyBudget = { category, budget -> viewModel.calculateDailyBudget(category, budget) },
            calculateBusRides = viewModel::calculateBusRides
        )

        completedAdapter = CategoryAdapter(
            onCategoryClick = { categoryData ->
                showAddTransactionDialog(categoryData.first)
            },
            onCategoryLongClick = { categoryData ->
                showEditCategoryDialog(categoryData)
                true  // Return true to indicate the long click was handled
            },
            // FIXED: Add the new listener
            onAddCategoryClick = {
                // You probably don't want an "Add" button on the *completed* list
                // but we'll wire it up just in case.
                showCreateCategoryDialog()
            },
            calculateDailyBudget = { category, budget -> viewModel.calculateDailyBudget(category, budget) },
            calculateBusRides = viewModel::calculateBusRides
        )

        binding.rvActiveCategories.adapter = activeAdapter
        binding.rvCompletedCategories.adapter = completedAdapter
    }

    private fun showEditCategoryDialog(categoryData: Triple<Category, Double, Double>) {
        val (category, _, budget) = categoryData
        val currentBudget = budget
        EditCategoryDialog(
            category = category,
            currentBudget = currentBudget,
            onCategoryEdited = { name, newBudget ->
                viewModel.updateCategory(
                    category.copy(name = name),
                    newBudget
                )
            },
            onCategoryDeleted = {
                viewModel.deleteCategory(category)
            },
            isNewCategory = false
        ).show(parentFragmentManager, EditCategoryDialog.TAG)
    }

    private fun setupObservers() {
        // This is all your existing logic, which is still correct
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentBalance.collectLatest { balance ->
                val format = NumberFormat.getCurrencyInstance(Locale.US)
                binding.tvCurrentBalance.text = format.format(balance)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalSpent.collectLatest { spent ->
                viewModel.totalBudget.collectLatest { budget ->
                    val format = NumberFormat.getCurrencyInstance(Locale.US)
                    binding.tvOverallBudget.text =
                        "${format.format(spent)} of ${format.format(budget)}"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeCategories.collectLatest { categories ->
                activeAdapter.submitList(categories)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.completedCategories.collectLatest { categories ->
                completedAdapter.submitList(categories)
                updateCompletedButton(categories.size)
            }
        }

        // --- ADDED: This logic was moved from MainActivity ---
        viewLifecycleOwner.lifecycleScope.launch {
            sharedMonthViewModel.selectedMonth.collectLatest { monthYear ->
                // The binding is direct, not via topBar
                binding.tvMonthSelector.text = monthYear.toDisplayString()
            }
        }
    }

    private fun setupClickListeners() {
        // This is your existing logic, which is still correct
        binding.btnToggleCompleted.setOnClickListener {
            isCompletedExpanded = !isCompletedExpanded
            binding.rvCompletedCategories.visibility =
                if (isCompletedExpanded) View.VISIBLE else View.GONE
            binding.btnToggleCompleted.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                if (isCompletedExpanded) R.drawable.ic_chevron_up
                else R.drawable.ic_chevron_down,
                0
            )
        }
        binding.btnAddIncome.setOnClickListener {
            showAddIncomeDialog()
        }

        // --- ADDED: This logic was moved from MainActivity ---
        binding.tvMonthSelector.setOnClickListener {
            showMonthSelector()
        }

        binding.btnSettings.setOnClickListener {
            showConfigureBalanceDialog()
        }
    }

    private fun showAddIncomeDialog() {
        AddIncomeDialog { amount, source, description, date ->
            viewModel.addIncome(amount, source, description, date)
        }.show(parentFragmentManager, "AddIncomeDialog")
    }

    // --- ADDED: This function was moved from MainActivity ---
    private fun showConfigureBalanceDialog() {
        ConfigureBalanceDialog().show(
            parentFragmentManager,
            ConfigureBalanceDialog.TAG
        )
    }

    // --- ADDED: This function was moved from MainActivity ---
    private fun showMonthSelector() {
        MonthSelectorDialog(
            currentSelection = sharedMonthViewModel.selectedMonth.value,
            archivedMonths = sharedMonthViewModel.archivedMonths.value,
            onMonthSelected = { selected ->
                println("Month selected in dialog: ${selected.toDisplayString()}")
                sharedMonthViewModel.selectMonth(selected)
            },
            onMonthArchived = { monthYear ->
                sharedMonthViewModel.archiveMonth(monthYear)
            },
            onMonthUnarchived = { monthYear ->
                sharedMonthViewModel.unarchiveMonth(monthYear)
            }
        ).show(parentFragmentManager, MonthSelectorDialog.TAG)
    }

    private fun updateCompletedButton(completedCount: Int) {
        binding.btnToggleCompleted.text = "Completed ($completedCount)"
        binding.layoutCompleted.visibility =
            if (completedCount > 0) View.VISIBLE else View.GONE
    }

    private fun showAddTransactionDialog(category: Category) {
        AddTransactionDialog(
            category = category,
            onTransactionAdded = { amount, merchant, description, date ->
                viewModel.addTransaction(
                    categoryId = category.id,
                    amount = amount,
                    merchant = merchant,
                    description = description,
                    date = date
                )
            }
        ).show(childFragmentManager, "AddTransactionDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}