package com.example.cleanarchitecture.domain.entity.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

fun String.isAValidName(): Boolean {
    return this.isNotEmpty() && matches(Regex("^[a-zA-Z\\s]+$"))
}
fun String.isAValidAddress(): Boolean {
    return !isNullOrEmpty() && matches(Regex("^[a-zA-Z0-9\\s.,-]+$"))
}

fun String.isValidIpv4(): Boolean {
    val pattern =
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\$"
    val ipv4Regex = Regex(pattern)
    return ipv4Regex.matches(this)
}

fun String?.isValidDni(): Boolean {
    val pattern = "^[0-9]{8}\$"
    val dniRegex = Regex(pattern)
    return dniRegex.matches(this ?: "")
}

fun String?.isValidDouble(): Boolean {
    return try {
        this!!.toDouble()
        true
    } catch (e: Exception) {
        false
    }
}

fun String?.isValidPhone(): Boolean {
    val pattern = "^[0-9]{9}\$"
    val phoneRegex = Regex(pattern)
    return phoneRegex.matches(this ?: "")
}

fun String?.isValidEmail(): Boolean {
    val pattern = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})\$"
    val emailRegex = Regex(pattern)
    return emailRegex.matches(this ?: "")
}

fun String?.IsValidIpv4Segment(): Boolean {
    if (this == null) return false
    val ipv4SegmentRegex = "^(\\d{1,3}\\.){3}\\d{1,3}/\\d{1,2}$".toRegex()
    return ipv4SegmentRegex.matches(this)
}

fun Long.toFormattedDateString(): String {
    val date = Date(this)
    val formatter = SimpleDateFormat("dd/MM/yyyy")
    return formatter.format(date)
}

fun Long.localToUTC(): Long {
    val offsetFromUtc = TimeZone.getDefault().getOffset(this)
    return this - offsetFromUtc
}

