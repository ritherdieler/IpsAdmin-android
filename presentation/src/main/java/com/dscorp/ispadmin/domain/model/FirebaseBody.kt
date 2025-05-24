package com.dscorp.ispadmin.domain.model

data class FirebaseBody(
    var to: String,
    var priority: String,
    var data: Map<String, String>,
    var time_to_live: Int
)
