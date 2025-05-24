package com.dscorp.ispadmin.data.extensions

import java.nio.charset.StandardCharsets
import java.security.MessageDigest


fun String.encryptWithSHA384(): String {
    val bytes = MessageDigest
        .getInstance("SHA-384")
        .digest(this.toByteArray(StandardCharsets.UTF_8))

    return bytes.fold("") { str, it -> str + "%02x".format(it) }
}

fun main() {
    println("cintia123".encryptWithSHA384())
}