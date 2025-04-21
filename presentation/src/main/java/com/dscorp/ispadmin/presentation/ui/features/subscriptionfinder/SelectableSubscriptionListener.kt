package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder

import android.view.View
import com.dscorp.ispadmin.domain.model.SubscriptionResponse

interface SelectableSubscriptionListener {
    fun onSubscriptionPopupButtonSelected(subscription: SubscriptionResponse, view: View)
}
