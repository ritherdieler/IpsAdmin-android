package com.dscorp.ispadmin.presentation.bindingadapter

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner

//visibility from boolean binding adapter
@BindingAdapter("android:visibility")
fun setVisibility(view: View, value: Boolean) {
    view.visibility = if (value) TextView.VISIBLE else TextView.GONE
}


@BindingAdapter("app:focusablem")
fun focusable(view: View, value: MutableLiveData<Boolean>) {

    val observer = Observer<Boolean> {
        view.isFocusable = it
        view.isFocusableInTouchMode = it
        view.isEnabled = it
    }

    view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            view.findViewTreeLifecycleOwner()?.let { value.observe(it, observer) }
        }

        override fun onViewDetachedFromWindow(v: View) {
            value.removeObserver(observer)
        }
    })
}


