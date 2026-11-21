package com.example.knowledgechallenge.ui.home

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.knowledgechallenge.KnowledgeApp
import com.example.knowledgechallenge.R
import com.example.knowledgechallenge.ads.AdState
import com.example.knowledgechallenge.data.DailyChallenge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RewardType { HINT, ATTEMPT }

data class HomeUiState(
    val hints: Int = 0,
    val attempts: Int = 0,
    val adState: AdState = AdState.Idle,
    val adsEnabled: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val streak: Int = 0,
    val dailyDone: Boolean = false,
    val message: String? = null
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val knowledgeApp = app as KnowledgeApp
    private val prefs = knowledgeApp.userPrefs
    private val adManager = knowledgeApp.rewardedAdManager
    private val consent = knowledgeApp.consentManager
    private val message = MutableStateFlow<String?>(null)

    private val balance = combine(prefs.hints, prefs.attempts, prefs.dailyState) { h, a, d ->
        Triple(h, a, d)
    }

    private val adInfo = combine(
        adManager.state, knowledgeApp.adsInitialized, consent.privacyOptionsRequired
    ) { state, initialized, privacy -> Triple(state, initialized, privacy) }

    val uiState: StateFlow<HomeUiState> = combine(balance, adInfo, message) { b, a, msg ->
        val (hints, attempts, daily) = b
        val (adState, adsEnabled, privacy) = a
        val today = DailyChallenge.todayEpochDay()
        HomeUiState(
            hints = hints,
            attempts = attempts,
            adState = adState,
            adsEnabled = adsEnabled,
            privacyOptionsRequired = privacy,
            streak = if (daily.lastCompletedDay >= today - 1) daily.streak else 0,
            dailyDone = daily.lastCompletedDay == today,
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onWatchAdClicked(activity: Activity, type: RewardType) {
        val started = adManager.show(activity) {
            viewModelScope.launch {
                when (type) {
                    RewardType.HINT -> {
                        prefs.addHints(1)
                        message.value = str(R.string.msg_hint_granted)
                    }
                    RewardType.ATTEMPT -> {
                        prefs.addAttempts(1)
                        message.value = str(R.string.msg_attempt_granted)
                    }
                }
            }
        }
        if (!started) {
            message.value = str(R.string.msg_ad_not_ready)
            adManager.load()
        }
    }

    fun onPrivacyOptionsClicked(activity: Activity) {
        consent.showPrivacyOptionsForm(activity)
    }

    fun messageShown() {
        message.value = null
    }

    private fun str(id: Int) = getApplication<Application>().getString(id)
}
