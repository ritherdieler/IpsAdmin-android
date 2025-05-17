package com.dscorp.ispadmin.presentation.ui.features.formvalidation

import android.content.Context
import androidx.lifecycle.MutableLiveData
import org.koin.java.KoinJavaComponent.inject

class ReactiveFormField<T>(
    hintResourceId: Int? = null,
    private val errorResourceId: Int? = null,
    isEditable: Boolean = true,
    private val validator: ((validation: T?) -> Boolean)? = null
) {

    private val applicationContext: Context by inject(Context::class.java)

    var liveData = CustomLiveData<T?>(null, onValueChanged = { validateField(it) })

    fun setValue(value: T?) {
        liveData.value = value
    }

    fun getValue() = liveData.value
    val hint: String? = hintResourceId?.let { applicationContext.getString(it) }

    val errorLiveData = MutableLiveData<String?>(null)

    val editableLiveData = MutableLiveData(isEditable)
    fun setEditable(isEditable: Boolean) {
        editableLiveData.value = isEditable
    }

    //Use this only for  enable or disable databinding views
    val isValidLiveData = MutableLiveData(false)

    fun isValid(): Boolean {
        val isValid = currentValueIsValid()
        if (!isValid) errorLiveData.value =
            errorResourceId?.let { applicationContext.getString(it) }
        return isValid
    }

    private fun currentValueIsValid() = validator?.let { it(liveData.value) } != false

    private fun validateField(fieldValue: T?): Boolean {
        return if (currentValueIsValid()) {
            errorLiveData.value = null
            isValidLiveData.value = true
            true
        } else {
            errorLiveData.value = errorResourceId?.let { applicationContext.getString(it) }
            isValidLiveData.value = false
            false
        }
    }
}

