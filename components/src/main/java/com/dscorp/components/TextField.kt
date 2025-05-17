package com.dscorp.components

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import com.dscorp.components.databinding.ViewEditTextBinding

class TextField @JvmOverloads constructor(
    private val context: Context,
    attrs: AttributeSet
) : LinearLayout(context, attrs) {

    private val binding = ViewEditTextBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    var textChangeListener: ((String) -> Unit)? = null
        set(value) {
            field = value
            binding.editText.addTextChangedListener {
                value?.invoke(it.toString())
            }
        }

    var inputText: String = ""
        set(value) {
            field = value
            binding.editText.setText(value)
        }

    var hint: String? = null
        set(value) {
            field = value
            binding.editText.hint = value
        }

    var error: String? = null
        set(value) {
            field = value
            binding.editText.error = value
        }

    var inputType: Int = InputType.TYPE_CLASS_TEXT
        set(value) {
            field = value
            binding.editText.inputType = value
        }

    var imeActionListener: (() -> Unit)? = null
        set(value) {
            field = value
            binding.editText.setOnEditorActionListener { _, _, _ ->
                value?.invoke()
                true
            }
        }

    var textFilters: Array<InputFilter>? = null
        set(value) {
            field = value
            binding.editText.filters = value
        }

    init {
        loadAttributes()
    }

    private fun loadAttributes() {
        val attributes = context.obtainStyledAttributes(R.styleable.EditText)
        inputText = attributes.getString(R.styleable.EditText_inputText)?:""
        hint = attributes.getString(R.styleable.EditText_hint)
        error = attributes.getString(R.styleable.EditText_error)
        inputType = attributes.getInt(R.styleable.EditText_inputType, InputType.TYPE_CLASS_TEXT)
        attributes.recycle()
    }


    @BindingAdapter("app:inputText")
    fun setText(view: TextField, text: String?) {
        view.inputText = text?: ""
    }

}