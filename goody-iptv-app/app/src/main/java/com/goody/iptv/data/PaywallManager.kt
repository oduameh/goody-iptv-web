package com.goody.iptv.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.paywallDataStore by preferencesDataStore("paywall_prefs")

class PaywallManager(private val context: Context) {
    companion object {
        private val TRIAL_START_TIME = longPreferencesKey("trial_start_time")
        private val IS_UNLOCKED = booleanPreferencesKey("is_unlocked")
        const val TRIAL_DURATION_MS = 10 * 60 * 1000L // 10 minutes
        val UNLOCK_CODES = setOf("GOODY2024", "PREMIUM123", "UNLOCK456")
    }

    val isUnlocked: Flow<Boolean> = context.paywallDataStore.data.map { it[IS_UNLOCKED] ?: false }
    val trialStartTime: Flow<Long> = context.paywallDataStore.data.map { it[TRIAL_START_TIME] ?: 0L }

    suspend fun startTrial() {
        context.paywallDataStore.edit { prefs ->
            if (prefs[TRIAL_START_TIME] == null || prefs[TRIAL_START_TIME] == 0L) {
                prefs[TRIAL_START_TIME] = System.currentTimeMillis()
            }
        }
    }

    suspend fun unlock(code: String): Boolean {
        if (UNLOCK_CODES.contains(code.uppercase().trim())) {
            context.paywallDataStore.edit { prefs ->
                prefs[IS_UNLOCKED] = true
            }
            return true
        }
        return false
    }

    suspend fun isTrialExpired(): Boolean {
        val unlocked = context.paywallDataStore.data.map { it[IS_UNLOCKED] ?: false }
        val startTime = context.paywallDataStore.data.map { it[TRIAL_START_TIME] ?: 0L }
        
        // If unlocked, trial never expires
        if (unlocked.map { it }.firstOrNull() == true) return false
        
        val start = startTime.map { it }.firstOrNull() ?: 0L
        if (start == 0L) return false
        
        val elapsed = System.currentTimeMillis() - start
        return elapsed >= TRIAL_DURATION_MS
    }

    fun getTrialTimeRemaining(startTime: Long): Long {
        val elapsed = System.currentTimeMillis() - startTime
        return kotlin.math.max(0L, TRIAL_DURATION_MS - elapsed)
    }
} 