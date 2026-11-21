package com.example.knowledgechallenge.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * يدير موافقة المستخدم (GDPR / سياسات الخصوصية) عبر Google UMP.
 * لا نطلب أي إعلان ولا نهيّئ AdMob قبل أن يسمح canRequestAds().
 */
class ConsentManager(context: Context) {
    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val _privacyOptionsRequired = MutableStateFlow(isPrivacyOptionsRequired())

    /** هل يجب إظهار زر "إعدادات الخصوصية" (يتطلبه النظام في مناطق مثل الاتحاد الأوروبي). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    fun canRequestAds(): Boolean = info.canRequestAds()

    private fun isPrivacyOptionsRequired() =
        info.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** يُستدعى عند كل تشغيل للتطبيق. يعرض نموذج الموافقة تلقائيًا إن لزم، ثم يستدعي [onComplete]. */
    fun gatherConsent(activity: Activity, onComplete: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    _privacyOptionsRequired.value = isPrivacyOptionsRequired()
                    onComplete()
                }
            },
            { _ ->
                _privacyOptionsRequired.value = isPrivacyOptionsRequired()
                onComplete()
            }
        )
    }

    /** يفتح نموذج تغيير الموافقة (من زر "إعدادات الخصوصية"). */
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
            _privacyOptionsRequired.value = isPrivacyOptionsRequired()
            onDismissed()
        }
    }
}
