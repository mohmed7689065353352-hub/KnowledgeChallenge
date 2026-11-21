package com.example.knowledgechallenge.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * يدير الإعلان البيني. يُعرض فقط عند نقطة انتقال طبيعية (بعد إنهاء جولة كاملة).
 * - سقف تكرار: لا يظهر أكثر من مرة كل MIN_INTERVAL_MS.
 * - إن لم يكن الإعلان جاهزًا أو لم يحن وقته، يُكمل التطبيق مباشرة دون أي تأخير.
 */
class InterstitialAdManager(private val appContext: Context) {
    private var interstitialAd: InterstitialAd? = null
    private var retryCount = 0
    private var lastShownAt = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(AdState.Idle)
    val state: StateFlow<AdState> = _state.asStateFlow()

    /** يحدده KnowledgeApp: لا نحمّل أي إعلان قبل تهيئة AdMob بعد الموافقة. */
    var canLoad: () -> Boolean = { true }

    /** يُستدعى على الخيط الرئيسي. */
    fun load() {
        if (!canLoad() || _state.value != AdState.Idle) return
        _state.value = AdState.Loading
        InterstitialAd.load(
            appContext,
            AdIds.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    retryCount = 0
                    _state.value = AdState.Ready
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _state.value = AdState.Idle
                    scheduleRetry()
                }
            }
        )
    }

    private fun scheduleRetry() {
        if (retryCount >= MAX_RETRIES) return
        retryCount++
        mainHandler.postDelayed({ load() }, 2000L * (1L shl retryCount))
    }

    /**
     * يعرض الإعلان إن كان جاهزًا وسمح سقف التكرار، ثم يستدعي [onFinished].
     * وإلا يستدعي [onFinished] فورًا. يُستدعى [onFinished] مرة واحدة دائمًا.
     */
    fun showIfAvailable(activity: Activity, onFinished: () -> Unit) {
        val ad = interstitialAd
        val now = SystemClock.elapsedRealtime()
        val intervalPassed = lastShownAt == 0L || now - lastShownAt >= MIN_INTERVAL_MS
        if (ad == null || !intervalPassed) {
            load()
            onFinished()
            return
        }
        interstitialAd = null
        _state.value = AdState.Idle
        lastShownAt = now
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                retryCount = 0
                load()
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                load()
                onFinished()
            }
        }
        ad.show(activity)
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val MIN_INTERVAL_MS = 120_000L // دقيقتان
    }
}
