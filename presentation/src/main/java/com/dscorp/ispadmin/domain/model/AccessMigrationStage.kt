package com.dscorp.ispadmin.domain.model

enum class AccessMigrationStage {
    ELIGIBLE,
    OLT_READY,
    SECRET_READY,
    CPE_APPLIED,
    VERIFIED,
    QUEUE_CLEARED,
    QUARANTINE,
    DONE,
    FAILED_REVERTED,
    FAILED_STRANDED;

    fun isTerminal(): Boolean = this == QUARANTINE ||
        this == DONE ||
        this == FAILED_REVERTED ||
        this == FAILED_STRANDED

    fun isFailure(): Boolean = this == FAILED_REVERTED || this == FAILED_STRANDED

    companion object {
        fun parse(raw: String?): AccessMigrationStage? =
            entries.find { it.name.equals(raw, ignoreCase = true) }
    }
}
