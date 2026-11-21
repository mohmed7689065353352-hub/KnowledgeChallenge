package com.example.knowledgechallenge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPrefs(private val context: Context) {
    private object Keys {
        val HINTS = intPreferencesKey("hints")
        val ATTEMPTS = intPreferencesKey("attempts")
        val DAILY_LAST_DAY = longPreferencesKey("daily_last_day")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
    }

    val hints: Flow<Int> = context.dataStore.data.map { it[Keys.HINTS] ?: DEFAULT_HINTS }
    val attempts: Flow<Int> = context.dataStore.data.map { it[Keys.ATTEMPTS] ?: DEFAULT_ATTEMPTS }
    val dailyState: Flow<DailyState> = context.dataStore.data.map {
        DailyState(it[Keys.DAILY_LAST_DAY] ?: -1L, it[Keys.DAILY_STREAK] ?: 0)
    }

    suspend fun addHints(amount: Int) {
        context.dataStore.edit { it[Keys.HINTS] = (it[Keys.HINTS] ?: DEFAULT_HINTS) + amount }
    }

    suspend fun addAttempts(amount: Int) {
        context.dataStore.edit { it[Keys.ATTEMPTS] = (it[Keys.ATTEMPTS] ?: DEFAULT_ATTEMPTS) + amount }
    }

    suspend fun consumeHint(): Boolean {
        var consumed = false
        context.dataStore.edit {
            val current = it[Keys.HINTS] ?: DEFAULT_HINTS
            if (current > 0) {
                it[Keys.HINTS] = current - 1
                consumed = true
            }
        }
        return consumed
    }

    suspend fun consumeAttempt(): Boolean {
        var consumed = false
        context.dataStore.edit {
            val current = it[Keys.ATTEMPTS] ?: DEFAULT_ATTEMPTS
            if (current > 0) {
                it[Keys.ATTEMPTS] = current - 1
                consumed = true
            }
        }
        return consumed
    }

    /**
     * يسجّل إنجاز التحدي اليومي ويمنح المكافأة وفي عملية واحدة ذرّية.
     * إن كان اليوم مسجّلًا من قبل يعيد مكافأة فارغة (لا مكافأة مكررة).
     */
    suspend fun completeDaily(today: Long, score: Int, total: Int): DailyReward {
        var reward = DailyReward()
        context.dataStore.edit { p ->
            val last = p[Keys.DAILY_LAST_DAY] ?: -1L
            if (last == today) return@edit
            val newStreak = if (last == today - 1) (p[Keys.DAILY_STREAK] ?: 0) + 1 else 1
            val hintsGain = DAILY_HINT_REWARD
            val attemptsGain = if (total > 0 && score == total) DAILY_PERFECT_ATTEMPT_REWARD else 0
            p[Keys.DAILY_LAST_DAY] = today
            p[Keys.DAILY_STREAK] = newStreak
            p[Keys.HINTS] = (p[Keys.HINTS] ?: DEFAULT_HINTS) + hintsGain
            if (attemptsGain > 0) {
                p[Keys.ATTEMPTS] = (p[Keys.ATTEMPTS] ?: DEFAULT_ATTEMPTS) + attemptsGain
            }
            reward = DailyReward(newStreak, hintsGain, attemptsGain)
        }
        return reward
    }

    companion object {
        const val DEFAULT_HINTS = 3
        const val DEFAULT_ATTEMPTS = 3
        const val DAILY_HINT_REWARD = 1
        const val DAILY_PERFECT_ATTEMPT_REWARD = 1
    }
}
