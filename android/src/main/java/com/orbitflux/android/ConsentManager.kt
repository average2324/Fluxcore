package com.orbitflux.android

import android.app.Activity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

class ConsentManager(private val activity: Activity) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    fun gatherConsent(onComplete: (Boolean) -> Unit) {
        val requestParametersBuilder = ConsentRequestParameters.Builder()

        // Debug geography can be enabled locally with ORBITFLUX_DEBUG_EEA=1.
        if (System.getenv("ORBITFLUX_DEBUG_EEA") == "1") {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build()
            requestParametersBuilder.setConsentDebugSettings(debugSettings)
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            requestParametersBuilder.build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _: FormError? ->
                    onComplete(consentInformation.canRequestAds())
                }
            },
            {
                onComplete(consentInformation.canRequestAds())
            }
        )
    }
}
