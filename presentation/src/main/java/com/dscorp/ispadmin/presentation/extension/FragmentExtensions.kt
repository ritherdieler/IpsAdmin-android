package com.dscorp.ispadmin.presentation.extension

import android.app.Activity
import android.util.Log
import androidx.fragment.app.Fragment

fun Fragment.showCurrentSimpleName() {
    Log.w("CSN", (this::class.simpleName.toString()))
}

fun Activity.showCurrentSimpleName() {
    Log.w("CSN", (this::class.simpleName.toString()))
}

