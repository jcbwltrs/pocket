// File: data/preferences/UserPreferences.kt
package com.jcbwltrs.budgettracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {
    private val startingBalanceKey = doublePreferencesKey("starting_balance")

    val startingBalance: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[startingBalanceKey] ?: 0.0
        }

    suspend fun saveStartingBalance(amount: Double) {
        context.dataStore.edit { preferences ->
            preferences[startingBalanceKey] = amount
        }
    }
}