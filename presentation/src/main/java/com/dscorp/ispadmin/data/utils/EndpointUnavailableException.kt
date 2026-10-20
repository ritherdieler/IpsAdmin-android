package com.dscorp.ispadmin.data.utils

/** The Core answering does not have this endpoint yet (see [ApiErrorBodyParser.isMissingEndpoint]). */
class EndpointUnavailableException : Exception("Endpoint not available on this Core")
