package com.example.knowledgechallenge.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AdState { Idle, Loading, Ready }

/**
 * يدير تحميل وعرض إعلان المكافأة.
 * - يحمّل الإعلان مسبقًا ليكون جاهزًا عند طلب المستخدم.
 * - لا يمنح المكافأة إلا عند استلام onUserEarnedReward (أي إكمال المشاهدة).
 * - لا يحتفظ بمرجع لأي Activity (تفاديًا لتسريب الذاكرة).
 */
class RewardedAdManager(private val appContext: Context) {
    private var rewardedAd: RewardedAd? = null
    private var retryCount = 0
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(AdState.Idle)
    val state: StateFlow<AdState> = _state.asStateFlow()

    /** يحدده KnowledgeApp: لا نحمّل أي إعلان قبل تهيئة AdMob بعد الموافقة. */
    var canLoad: () -> Boolean = { true }

    /** يُستدعى على الخيط الرئيسي. */
    fun load() {
        if (!canLoad() || _state.value != AdState.Idle) return
        _state.value = AdState.Loading
        RewardedAd.load(
            appContext,
            AdIds.REWARDED,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    retryCount = 0
                    _state.value = AdState.Ready
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _state.value = AdState.Idle
                    scheduleRetry()
                }
            }
        )
    }

    /** إعادة محاولة بتأخير تصاعدي (4ث، 8ث، 16ث) كحد أقصى 3 مرات لتجنب الطلبات المفرطة. */
    private fun scheduleRetry() {
        if (retryCount >= MAX_RETRIES) return
        retryCount++
        val delayMs = 2000L * (1L shl retryCount)
        mainHandler.postDelayed({ load() }, delayMs)
    }

    /**
     * يعرض الإعلان إن كان جاهزًا.
     * @return true إذا بدأ العرض، false إذا لم يكن الإعلان جاهزًا.
     */
    fun show(activity: Activity, onRewardEarned: () -> Unit): Boolean {
        val ad = rewardedAd ?: return false
        rewardedAd = null
        _state.value = AdState.Idle
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                retryCount = 0
                load()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                load()
            }
        }
        ad.show(activity) { _ -> onRewardEarned() }
        return true
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}
