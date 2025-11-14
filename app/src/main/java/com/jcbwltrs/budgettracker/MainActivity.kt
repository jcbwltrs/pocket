package com.jcbwltrs.budgettracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.jcbwltrs.budgettracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // REMOVED: ViewModel and all related logic

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        // REMOVED: setupMonthSelector()
        // REMOVED: setupSettingsButton()
        // REMOVED: observeSelectedMonth()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)
    }

    // REMOVED: All other functions (setupMonthSelector, observeSelectedMonth, showMonthSelector, setupSettingsButton, showConfigureBalanceDialog)
    // This logic now lives in the fragments.
}