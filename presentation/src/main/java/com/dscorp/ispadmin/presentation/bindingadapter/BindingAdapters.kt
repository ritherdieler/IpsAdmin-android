package com.dscorp.ispadmin.presentation.bindingadapter

import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.formvalidation.ReactiveFormField
import com.dscorp.ispadmin.domain.model.PlaceResponse
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

//visibility from boolean binding adapter
@BindingAdapter("android:visibility")
fun setVisibility(view: View, value: Boolean) {
    view.visibility = if (value) TextView.VISIBLE else TextView.GONE
}


@BindingAdapter("app:imeActionListener")
fun setImeActionListener(view: TextInputEditText, listener: () -> Unit) {
    view.setOnEditorActionListener { _, _, _ ->
        listener()
        true
    }
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


@BindingAdapter("app:icon")
fun changeFabIcon(fab: FloatingActionButton, value: MutableLiveData<Int>) {

    val observer = Observer<Int> {
        fab.setImageResource(it)
    }

    fab.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            fab.findViewTreeLifecycleOwner()?.let { value.observe(it, observer) }
        }

        override fun onViewDetachedFromWindow(v: View) {
            value.removeObserver(observer)
        }
    })
}


@BindingAdapter(value = ["app:places", "app:fieldToSaveSelection"], requireAll = true)
fun com.google.android.material.textfield.MaterialAutoCompleteTextView.fillPlacesAutoComplete(
    places: MutableLiveData<List<PlaceResponse>>,
    fieldToSaveSelection: ReactiveFormField<PlaceResponse>
) {
    val adapter = ArrayAdapter(
        context,
        android.R.layout.simple_list_item_1,
        places.value ?: emptyList<PlaceResponse>()
    )
    setAdapter(adapter)

    val observer = Observer<List<PlaceResponse>> {
//        adapter.clear()
        adapter.addAll(it)
    }

    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            findViewTreeLifecycleOwner()?.let { places.observe(it, observer) }
        }

        override fun onViewDetachedFromWindow(v: View) {
            places.removeObserver(observer)
        }
    })

    setOnItemClickListener { parent, view, position, id ->
        val place = parent.getItemAtPosition(position) as PlaceResponse
        fieldToSaveSelection.setValue(place)
    }

}

@BindingAdapter(value = ["app:location"])
fun TextInputEditText.fillLocation(
    location: MutableLiveData<LatLng>
) {
    val observer = Observer<LatLng?> {
        setText("${it?.latitude}, ${it?.longitude}")
    }

    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            findViewTreeLifecycleOwner()?.let { location.observe(it, observer) }
        }

        override fun onViewDetachedFromWindow(v: View) {
            location.removeObserver(observer)
        }
    })
}

