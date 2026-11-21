package com.example.knowledgechallenge.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.knowledgechallenge.data.DailyChallenge
import com.example.knowledgechallenge.ui.home.HomeScreen
import com.example.knowledgechallenge.ui.quiz.QuizScreen
import com.example.knowledgechallenge.ui.summary.SummaryScreen

object Routes {
    const val HOME = "home"
    const val QUIZ = "quiz/{category}"
    const val SUMMARY = "summary/{category}/{score}/{total}?hints={hints}&attempts={attempts}"

    fun quiz(categoryId: String) = "quiz/$categoryId"
    fun summary(categoryId: String, score: Int, total: Int, hints: Int, attempts: Int) =
        "summary/$categoryId/$score/$total?hints=$hints&attempts=$attempts"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onCategoryClick = { navController.navigate(Routes.quiz(it.id)) },
                onQuickChallenge = { navController.navigate(Routes.quiz("mixed")) },
                onDailyChallenge = { navController.navigate(Routes.quiz(DailyChallenge.ID)) }
            )
        }
        composable(
            route = Routes.QUIZ,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) {
            QuizScreen(
                onExit = { navController.popBackStack() },
                onRoundFinished = { categoryId, score, total, hints, attempts ->
                    navController.navigate(Routes.summary(categoryId, score, total, hints, attempts)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        composable(
            route = Routes.SUMMARY,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType },
                navArgument("total") { type = NavType.IntType },
                navArgument("hints") { type = NavType.IntType; defaultValue = 0 },
                navArgument("attempts") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { entry ->
            val args = entry.arguments
            val categoryId = args?.getString("category") ?: "mixed"
            val isDaily = categoryId == DailyChallenge.ID
            SummaryScreen(
                score = args?.getInt("score") ?: 0,
                total = args?.getInt("total") ?: 0,
                isDaily = isDaily,
                rewardHints = args?.getInt("hints") ?: 0,
                rewardAttempts = args?.getInt("attempts") ?: 0,
                onReplay = {
                    val target = if (isDaily) "mixed" else categoryId
                    navController.navigate(Routes.quiz(target)) { popUpTo(Routes.HOME) }
                },
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }
    }
}
