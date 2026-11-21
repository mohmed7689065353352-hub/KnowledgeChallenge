package com.example.knowledgechallenge.data

import java.util.TimeZone

object DailyChallenge {
    const val ID = "daily"

    /** رقم اليوم المحلي منذ 1970 (يتغير عند منتصف الليل بتوقيت الجهاز). */
    fun todayEpochDay(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }
}

data class DailyState(val lastCompletedDay: Long = -1L, val streak: Int = 0)

data class DailyReward(
    val streak: Int = 0,
    val hintsGained: Int = 0,
    val attemptsGained: Int = 0
)
