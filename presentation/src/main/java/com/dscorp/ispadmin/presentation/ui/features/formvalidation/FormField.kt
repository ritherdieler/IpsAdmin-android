package com.dscorp.ispadmin.presentation.ui.features.formvalidation

import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import org.koin.java.KoinJavaComponent.inject

class FormField<T>(
    private val hintResourceId: Int,
    private val errorResourceId: Int,
    private val fieldValidator: FieldValidator<T>,
) {


    private val applicationContext: Context by inject(Context::class.java)

    var value: T? = null
        set(value) {
            field = value
            validateField(value)
        }

    val hint: String = applicationContext.getString(hintResourceId)

    val errorLiveData = MutableLiveData<String?>(null)

    //Use this only for  enable or disable databinding views
    val isValidObservable = ObservableBoolean(false)

    var isValid: Boolean = false
        get() {
            if (!field) errorLiveData.value = applicationContext.getString(errorResourceId)
            return field
        }

    fun emitErrorIfExists() = isValid

    private fun validateField(fieldValue: T?): Boolean {
        return if (!fieldValidator.validate(fieldValue)) {
            errorLiveData.value = applicationContext.getString(errorResourceId)
            isValid = false
            isValidObservable.set(false)
            false
        } else {
            errorLiveData.value = null
            isValid = true
            isValidObservable.set(true)
            true
        }
    }
}

