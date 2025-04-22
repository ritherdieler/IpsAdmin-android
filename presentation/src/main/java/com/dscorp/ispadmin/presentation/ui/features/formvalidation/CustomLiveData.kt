package com.dscorp.ispadmin.presentation.ui.features.formvalidation

import android.util.Log
import androidx.lifecycle.MutableLiveData

 class CustomLiveData<T>(initialValue: T?, val onValueChanged: (newValue: T?) -> Unit={}) :
    MutableLiveData<T>(initialValue) {
    override fun setValue(value: T?) {
        super.setValue(value)
        Log.d("MyTag", "El valor de MyMutableLiveData ha cambiado a: $value")
        onValueChanged.invoke(value)
    }
}
