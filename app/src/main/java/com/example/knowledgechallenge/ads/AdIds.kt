package com.example.knowledgechallenge.ads

import com.example.knowledgechallenge.BuildConfig

/** تُحدَّد المعرّفات من build.gradle.kts حسب نوع البناء (debug = اختبار، release = حقيقي). */
object AdIds {
    val REWARDED: String = BuildConfig.REWARDED_ID
    val INTERSTITIAL: String = BuildConfig.INTERSTITIAL_ID
}
