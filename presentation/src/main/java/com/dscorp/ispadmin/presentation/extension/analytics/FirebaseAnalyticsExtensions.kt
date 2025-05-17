package com.dscorp.ispadmin.presentation.extension.analytics

import android.os.Bundle
import com.dscorp.ispadmin.presentation.extension.analytics.AnalyticsConstants.BUTTON_CONTENT_TYPE
import com.google.firebase.analytics.FirebaseAnalytics

fun FirebaseAnalytics.sendTouchButtonEvent(event: String) {
    logEvent(
        FirebaseAnalytics.Event.SELECT_ITEM,
        Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_NAME, "touch_on_$event")
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, BUTTON_CONTENT_TYPE)
        }
    )
}

fun FirebaseAnalytics.sendSignUpEvent(event: String) {
    logEvent(
        FirebaseAnalytics.Event.SIGN_UP,
        Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_NAME, "touch_on_$event")
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, BUTTON_CONTENT_TYPE)
        }
    )
}

fun FirebaseAnalytics.sendScreen(event: String) {
    logEvent(
        FirebaseAnalytics.Event.SCREEN_VIEW,
        Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, event)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, event)
        }
    )
}
