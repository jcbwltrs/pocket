package com.jcbwltrs.budgettracker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.jcbwltrs.budgettracker.databinding.ActivityMainBinding
import com.jcbwltrs.budgettracker.ui.SharedMonthViewModel
import com.jcbwltrs.budgettracker.ui.ViewModelFactory
import com.jcbwltrs.budgettracker.ui.dialog.ConfigureBalanceDialog
import com.jcbwltrs.budgettracker.ui.dialog.MonthSelectorDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: SharedMonthViewModel by lazy {
        (application as BudgetApplication).sharedMonthViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupMonthSelector()
        setupSettingsButton()
        observeSelectedMonth()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)
    }

    private fun setupMonthSelector() {
        binding.topBar.tvMonthSelector.setOnClickListener {
            showMonthSelector()
        }
    }

    private fun observeSelectedMonth() {
        lifecycleScope.launch {
            viewModel.selectedMonth.collectLatest { monthYear ->
                binding.topBar.tvMonthSelector.text = monthYear.toDisplayString()
            }
        }
    }

    private fun showMonthSelector() {
        MonthSelectorDialog(
            currentSelection = viewModel.selectedMonth.value,
            archivedMonths = viewModel.archivedMonths.value,
            onMonthSelected = { selected ->
                println("Month selected in dialog: ${selected.toDisplayString()}")
                viewModel.selectMonth(selected)
            },
            onMonthArchived = { monthYear ->
                viewModel.archiveMonth(monthYear)
            },
            onMonthUnarchived = { monthYear ->
                viewModel.unarchiveMonth(monthYear)
            }
        ).show(supportFragmentManager, MonthSelectorDialog.TAG)
    }
    private fun setupSettingsButton() {
        binding.topBar.btnSettings.setOnClickListener {
            showConfigureBalanceDialog()
        }
    }

    private fun showConfigureBalanceDialog() {
        ConfigureBalanceDialog().show(
            supportFragmentManager,
            ConfigureBalanceDialog.TAG
        )
    }
}