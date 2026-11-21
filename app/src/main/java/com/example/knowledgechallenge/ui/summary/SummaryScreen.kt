package com.example.knowledgechallenge.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.knowledgechallenge.R

@Composable
fun SummaryScreen(
    score: Int,
    total: Int,
    isDaily: Boolean,
    rewardHints: Int,
    rewardAttempts: Int,
    onReplay: () -> Unit,
    onHome: () -> Unit,
    viewModel: SummaryViewModel = viewModel()
) {
    val hints by viewModel.hints.collectAsStateWithLifecycle()
    val attempts by viewModel.attempts.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()

    val ratio = if (total == 0) 0f else score.toFloat() / total
    val (emoji, messageRes) = when {
        ratio >= 0.8f -> "🏆" to R.string.summary_great
        ratio >= 0.5f -> "👏" to R.string.summary_good
        else -> "🌱" to R.string.summary_try
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            Text(emoji, fontSize = 64.sp)
            Text(
                stringResource(R.string.summary_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.summary_score, score, total),
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            if (isDaily) {
                Text(
                    text = stringResource(R.string.daily_streak, streak),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (rewardHints > 0 || rewardAttempts > 0) {
                Text(
                    text = stringResource(R.string.daily_reward, rewardHints, rewardAttempts),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        stringResource(R.string.summary_balance),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "💡 $hints      🎯 $attempts",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Button(
                onClick = onReplay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text(stringResource(R.string.summary_replay)) }
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text(stringResource(R.string.summary_home)) }
        }
    }
}
