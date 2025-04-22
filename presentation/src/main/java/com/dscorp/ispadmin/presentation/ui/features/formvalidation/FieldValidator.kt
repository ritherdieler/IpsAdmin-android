package com.dscorp.ispadmin.presentation.ui.features.formvalidation

interface FieldValidator<T> {
    fun validate(fieldValue: T?):Boolean
}