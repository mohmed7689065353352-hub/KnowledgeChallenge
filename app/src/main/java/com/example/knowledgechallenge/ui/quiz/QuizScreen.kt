package com.example.knowledgechallenge.ui.quiz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.knowledgechallenge.R
import com.example.knowledgechallenge.ui.util.findActivity

private enum class OptionVisual { Normal, Correct, Wrong, Eliminated }

@Composable
fun QuizScreen(
    onExit: () -> Unit,
    onRoundFinished: (categoryId: String, score: Int, total: Int, hints: Int, attempts: Int) -> Unit,
    viewModel: QuizViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val question = state.current ?: return
    val answered = state.status != AnswerStatus.Unanswered
    val progressTarget =
        (state.currentIndex + if (state.status == AnswerStatus.Correct) 1 else 0).toFloat() / state.total
    val progress by animateFloatAsState(progressTarget, label = "quiz_progress")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onExit) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(
                    text = "💡 ${state.hints}     🎯 ${state.attempts}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.quiz_question_counter, state.currentIndex + 1, state.total),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = question.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            question.options.forEachIndexed { index, option ->
                val visual = when {
                    index == state.selectedIndex && state.status == AnswerStatus.Correct -> OptionVisual.Correct
                    index == state.selectedIndex && state.status == AnswerStatus.Wrong -> OptionVisual.Wrong
                    index in state.wrongTried -> OptionVisual.Wrong
                    index in state.eliminated -> OptionVisual.Eliminated
                    state.revealCorrect && index == question.correctIndex -> OptionVisual.Correct
                    else -> OptionVisual.Normal
                }
                OptionItem(
                    text = option,
                    visual = visual,
                    enabled = !answered && visual == OptionVisual.Normal,
                    onClick = { viewModel.onOptionSelected(index) }
                )
            }

            when (state.status) {
                AnswerStatus.Unanswered -> {
                    OutlinedButton(
                        onClick = viewModel::onUseHint,
                        enabled = state.eliminated.isEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.quiz_hint_button))
                    }
                }
                AnswerStatus.Correct -> {
                    FeedbackText(stringResource(R.string.quiz_correct), Color(0xFF16A34A))
                }
                AnswerStatus.Wrong -> {
                    FeedbackText(stringResource(R.string.quiz_wrong), Color(0xFFDC2626))
                    if (state.attempts > 0) {
                        Button(
                            onClick = viewModel::onUseAttempt,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.quiz_use_attempt))
                        }
                    }
                }
            }

            if (answered) {
                val isLast = state.isLast
                val nextModifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                val label = stringResource(if (isLast) R.string.quiz_finish else R.string.quiz_next)
                val onClick: () -> Unit = {
                    if (isLast) {
                        activity?.let { act ->
                            viewModel.onFinishClicked(act) { score, total, h, a ->
                                onRoundFinished(viewModel.categoryId, score, total, h, a)
                            }
                        }
                    } else {
                        viewModel.onNext()
                    }
                }
                if (state.status == AnswerStatus.Wrong && state.attempts > 0) {
                    OutlinedButton(onClick = onClick, modifier = nextModifier) { Text(label) }
                } else {
                    Button(onClick = onClick, modifier = nextModifier) { Text(label) }
                }
            }
        }
    }
}

@Composable
private fun FeedbackText(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = color
    )
}

@Composable
private fun OptionItem(
    text: String,
    visual: OptionVisual,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val container = when (visual) {
        OptionVisual.Correct -> Color(0xFFDCFCE7)
        OptionVisual.Wrong -> Color(0xFFFEE2E2)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when (visual) {
        OptionVisual.Correct -> Color(0xFF16A34A)
        OptionVisual.Wrong -> Color(0xFFDC2626)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val textColor = when (visual) {
        OptionVisual.Correct, OptionVisual.Wrong -> Color(0xFF111827)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (visual == OptionVisual.Eliminated) 0.4f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            disabledContainerColor = container
        ),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = textColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
