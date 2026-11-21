package com.example.knowledgechallenge

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.example.knowledgechallenge.ads.ConsentManager
import com.example.knowledgechallenge.ads.InterstitialAdManager
import com.example.knowledgechallenge.ads.RewardedAdManager
import com.example.knowledgechallenge.data.UserPrefs
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class KnowledgeApp : Application() {
    lateinit var userPrefs: UserPrefs
        private set
    lateinit var consentManager: ConsentManager
        private set
    lateinit var rewardedAdManager: RewardedAdManager
        private set
    lateinit var interstitialAdManager: InterstitialAdManager
        private set

    private val initStarted = AtomicBoolean(false)
    private val _adsInitialized = MutableStateFlow(false)
    val adsInitialized: StateFlow<Boolean> = _adsInitialized.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        userPrefs = UserPrefs(this)
        consentManager = ConsentManager(this)
        rewardedAdManager = RewardedAdManager(this).also { it.canLoad = { _adsInitialized.value } }
        interstitialAdManager = InterstitialAdManager(this).also { it.canLoad = { _adsInitialized.value } }
    }

    /** يهيّئ AdMob مرة واحدة فقط، وفقط إذا سمحت الموافقة بطلب الإعلانات. */
    fun initializeAdsIfAllowed() {
        if (!consentManager.canRequestAds()) return
        if (!initStarted.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@KnowledgeApp) {
                Handler(Looper.getMainLooper()).post {
                    _adsInitialized.value = true
                    rewardedAdManager.load()
                    interstitialAdManager.load()
                }
            }
        }
    }
}
