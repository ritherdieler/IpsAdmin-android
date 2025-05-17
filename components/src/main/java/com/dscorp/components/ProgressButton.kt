package com.dscorp.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.postDelayed
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.dscorp.components.databinding.FragmentProgressButtonBinding

class ProgressButton @JvmOverloads constructor(
    context: Context, private val attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

     val binding: FragmentProgressButtonBinding = FragmentProgressButtonBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    private val firstClick = true
    var clickListener: () -> Unit = {}
        set(value) {
            field = value

            binding.button.setOnClickListener {
                if (isLoading) return@setOnClickListener
                if (firstClick) {
                    field.invoke()
                } else
                    Handler(Looper.getMainLooper()).postDelayed(700) {
                        field.invoke()
                    }
            }

        }
    var isLoading: Boolean = false
        set(value) {
            field = value
            if (field) {
                binding.progressBar.visibility = VISIBLE
                binding.button.textSize = 0f
            } else {
                binding.progressBar.visibility = GONE
                binding.button.textSize = 14f
            }
        }

    var text: String? = null
        set(value) {
            field = value
            binding.text = field
        }

    init {
        loadAttributes()
    }

    private fun loadAttributes() {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ProgressButton)
        isLoading = attributes.getBoolean(R.styleable.ProgressButton_isLoading, false)
        text = attributes.getString(R.styleable.ProgressButton_text)
        attributes.recycle()
    }
}

@BindingAdapter("app:clickListener")
fun onClickListener(button: ProgressButton, listener: () -> Unit) {
    button.clickListener = listener
}

@BindingAdapter("app:backGroundTint")
fun backGroundTint(button: ProgressButton, color: Int) {
    button.binding.button.setBackgroundColor(color)
}

@BindingAdapter("app:isLoading")
fun isLoading(button: ProgressButton, isLoading: MutableLiveData<Boolean>) {
    isLoading.observe(button.context as LifecycleOwner) {
        button.isLoading = it
    }
}