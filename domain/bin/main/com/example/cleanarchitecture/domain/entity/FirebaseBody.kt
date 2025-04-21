package com.example.cleanarchitecture.domain.entity

data class FirebaseBody(
    var to: String,
    var priority: String,
    var data: Map<String, String>,
    var time_to_live: Int
)
