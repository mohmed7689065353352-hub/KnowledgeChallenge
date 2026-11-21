package com.example.knowledgechallenge.ui.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.knowledgechallenge.KnowledgeApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SummaryViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = (app as KnowledgeApp).userPrefs
    val hints: StateFlow<Int> =
        prefs.hints.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val attempts: StateFlow<Int> =
        prefs.attempts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val streak: StateFlow<Int> =
        prefs.dailyState.map { it.streak }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
