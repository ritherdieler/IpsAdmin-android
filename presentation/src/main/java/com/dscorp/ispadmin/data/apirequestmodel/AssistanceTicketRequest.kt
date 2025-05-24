package com.dscorp.ispadmin.data.apirequestmodel

data class AssistanceTicketRequest(
    var phone: String = "",
    var category: String = "",
    var description: String = "",
    var subscriptionId: Int? = null,
    var placeName: String?,
    var customerName: String = "",
) {
    private fun isValidPhone(): Boolean {
        return phone.isNotEmpty() && phone.length == 9 // Por ejemplo, aquí se verifica que tenga 10 dígitos
    }

    private fun isValidCategory(): Boolean {
        return category.isNotEmpty() && category.length <= 255 // Por ejemplo, aquí se verifica que no esté vacía y no sea demasiado larga
    }

    private fun isValidDescription(): Boolean {

        return description.isNotEmpty() && description.length <= 300 // Por ejemplo, aquí se verifica que no esté vacía y no sea demasiado larga
    }

//    fun isValidSubscriptionId(): Boolean {
//        return subscriptionId >= 0 // Por ejemplo, aquí se verifica que sea un número no negativo
//    }

    fun isValid(): Boolean {
        return isValidPhone() && isValidCategory() && isValidDescription() && (subscriptionId != null || placeName != null)
    }
}