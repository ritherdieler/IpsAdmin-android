package com.example.cleanarchitecture.domain.entity

data class FireBaseResponse(
    var multicast_id: Long,
    var success: Int,
    var failure: Int,
    var canonical_ids: Int,
    var results: List<Any>
)
