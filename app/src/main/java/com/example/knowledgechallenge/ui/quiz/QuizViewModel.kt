package com.example.knowledgechallenge.ui.quiz

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.knowledgechallenge.KnowledgeApp
import com.example.knowledgechallenge.R
import com.example.knowledgechallenge.data.Category
import com.example.knowledgechallenge.data.DailyChallenge
import com.example.knowledgechallenge.data.DailyReward
import com.example.knowledgechallenge.data.Question
import com.example.knowledgechallenge.data.QuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AnswerStatus { Unanswered, Correct, Wrong }

data class QuizUiState(
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val selectedIndex: Int? = null,
    val status: AnswerStatus = AnswerStatus.Unanswered,
    val eliminated: Set<Int> = emptySet(),
    val wrongTried: Set<Int> = emptySet(),
    val score: Int = 0,
    val hints: Int = 0,
    val attempts: Int = 0,
    val message: String? = null
) {
    val current: Question? get() = questions.getOrNull(currentIndex)
    val total: Int get() = questions.size
    val isLast: Boolean get() = currentIndex >= questions.lastIndex
    val revealCorrect: Boolean get() = status == AnswerStatus.Wrong && attempts == 0
}

class QuizViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {
    private val knowledgeApp = app as KnowledgeApp
    private val prefs = knowledgeApp.userPrefs
    private val interstitial = knowledgeApp.interstitialAdManager

    val categoryId: String = savedStateHandle.get<String>("category") ?: "mixed"
    private val category: Category? = Category.fromId(categoryId)
    private val isDaily = categoryId == DailyChallenge.ID
    private val dailyDay = DailyChallenge.todayEpochDay()

    private val _state = MutableStateFlow(
        QuizUiState(
            questions = if (isDaily) QuestionRepository.getDailyRound(dailyDay, ROUND_SIZE)
            else QuestionRepository.getRound(category, ROUND_SIZE)
        )
    )
    val uiState: StateFlow<QuizUiState> = _state.asStateFlow()

    private var actionInFlight = false
    private var finishing = false

    init {
        interstitial.load()
        viewModelScope.launch {
            combine(prefs.hints, prefs.attempts) { h, a -> h to a }
                .collect { (h, a) -> _state.update { it.copy(hints = h, attempts = a) } }
        }
    }

    fun onOptionSelected(index: Int) {
        val s = _state.value
        val q = s.current ?: return
        if (s.status != AnswerStatus.Unanswered) return
        if (index in s.eliminated || index in s.wrongTried) return
        if (index == q.correctIndex) {
            _state.update {
                it.copy(selectedIndex = index, status = AnswerStatus.Correct, score = it.score + 1)
            }
        } else {
            _state.update {
                it.copy(
                    selectedIndex = index,
                    status = AnswerStatus.Wrong,
                    wrongTried = it.wrongTried + index
                )
            }
        }
    }

    fun onUseHint() {
        val s = _state.value
        val q = s.current ?: return
        if (s.status != AnswerStatus.Unanswered || s.eliminated.isNotEmpty()) return
        if (actionInFlight) return
        val candidates = q.options.indices.filter { it != q.correctIndex && it !in s.wrongTried }
        if (candidates.isEmpty()) return
        val questionIndex = s.currentIndex
        actionInFlight = true
        viewModelScope.launch {
            try {
                if (prefs.consumeHint()) {
                    val toRemove = candidates.shuffled().take(2).toSet()
                    _state.update {
                        if (it.currentIndex == questionIndex && it.status == AnswerStatus.Unanswered)
                            it.copy(eliminated = toRemove) else it
                    }
                } else {
                    _state.update { it.copy(message = str(R.string.msg_no_hints)) }
                }
            } finally {
                actionInFlight = false
            }
        }
    }

    fun onUseAttempt() {
        val s = _state.value
        if (s.status != AnswerStatus.Wrong || actionInFlight) return
        val questionIndex = s.currentIndex
        actionInFlight = true
        viewModelScope.launch {
            try {
                if (prefs.consumeAttempt()) {
                    _state.update {
                        if (it.currentIndex == questionIndex)
                            it.copy(selectedIndex = null, status = AnswerStatus.Unanswered) else it
                    }
                } else {
                    _state.update { it.copy(message = str(R.string.msg_no_attempts)) }
                }
            } finally {
                actionInFlight = false
            }
        }
    }

    fun onNext() {
        val s = _state.value
        if (s.status == AnswerStatus.Unanswered || s.isLast) return
        _state.update {
            it.copy(
                currentIndex = it.currentIndex + 1,
                selectedIndex = null,
                status = AnswerStatus.Unanswered,
                eliminated = emptySet(),
                wrongTried = emptySet()
            )
        }
    }

    fun onFinishClicked(
        activity: Activity,
        onResult: (score: Int, total: Int, hints: Int, attempts: Int) -> Unit
    ) {
        val s = _state.value
        if (s.status == AnswerStatus.Unanswered || !s.isLast || finishing) return
        finishing = true
        viewModelScope.launch {
            val reward = if (isDaily) prefs.completeDaily(dailyDay, s.score, s.total) else DailyReward()
            interstitial.showIfAvailable(activity) {
                onResult(s.score, s.total, reward.hintsGained, reward.attemptsGained)
            }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    private fun str(id: Int) = getApplication<Application>().getString(id)

    private companion object {
        const val ROUND_SIZE = 5
    }
}
