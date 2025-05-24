package com.dscorp.ispadmin.presentation.ui.features.forms.subscription

import com.dscorp.ispadmin.presentation.extension.formIsValid
import com.dscorp.ispadmin.presentation.extension.toGeoLocation
import com.dscorp.ispadmin.presentation.ui.features.formvalidation.ReactiveFormField
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.data.apirequestmodel.UpdateSubscriptionDataBody
import com.google.android.gms.maps.model.LatLng

class EditSubscriptionDataForm : SubscriptionForm() {


    private val fields = mutableListOf<ReactiveFormField<*>>()

    fun initForm(subscription: SubscriptionResponse) {

        idField.setValue(subscription.id.toString())

        firstNameField.apply {
            setValue(subscription.firstName)
            setEditable(false)
            fields.add(this)
        }

        lastNameField.apply {
            setValue(subscription.lastName)
            setEditable(false)
            fields.add(this)
        }
        dniField.apply {
            setValue(subscription.dni)
            setEditable(false)
            fields.add(this)
        }

        addressField.apply {
            setValue(subscription.address)
            setEditable(false)
            fields.add(this)

        }

        ipField.apply {
            setValue(subscription.ip)
            setEditable(false)
        }


        locationField.apply {
            setValue(subscription.location?.let {
                LatLng(
                    it.latitude,
                    it.longitude
                )
            })
            setEditable(false)
            fields.add(this)
        }

        phoneField.apply {
            setValue(subscription.phone)
            setEditable(false)
            fields.add(this)
        }

        planField.apply {
            setValue(subscription.plan)
            setEditable(false)
        }

        placeField.apply {
            setValue(subscription.place)
            setEditable(false)
            fields.add(this)
        }
        technicianField.apply {
            setValue(subscription.technician)
            setEditable(false)
        }

        hostDeviceField.apply {
            setValue(subscription.hostDevice)
            setEditable(false)
        }

        subscriptionDateField.apply {
            setValue(subscription.subscriptionDate)
            setEditable(false)
        }

        migrationField.apply {
            setValue(subscription.isMigration)
            setEditable(false)
        }

        priceField.apply {
            setValue(subscription.price.toString())
            setEditable(false)
        }

        noteField.apply {
            setValue(subscription.note)
            setEditable(false)
        }

    }

    fun changeEditableStatus(editing: Boolean) = fields.forEach {
        it.setEditable(editing)
    }

    fun getUpdateSubscriptionBody(): UpdateSubscriptionDataBody? {
        return if (fields.formIsValid()) {
            UpdateSubscriptionDataBody(
                subscriptionId = idField.getValue()!!.toInt(),
                firstName = firstNameField.getValue()!!,
                lastName = lastNameField.getValue()!!,
                dni = dniField.getValue()!!,
                address = addressField.getValue()!!,
                location = locationField.getValue()!!.toGeoLocation(),
                phone = phoneField.getValue()!!,
                placeId = placeField.getValue()!!.id!!
            )
        } else {
            null
        }
    }
}