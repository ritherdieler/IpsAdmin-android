package com.dscorp.ispadmin.domain.model

data class FireBaseResponse(
    var multicast_id: Long,
    var success: Int,
    var failure: Int,
    var canonical_ids: Int,
    var results: List<Any>
)
