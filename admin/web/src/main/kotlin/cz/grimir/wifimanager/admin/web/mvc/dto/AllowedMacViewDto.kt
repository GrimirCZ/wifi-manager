package cz.grimir.wifimanager.admin.web.mvc.dto

import java.time.Instant

data class AllowedMacViewDto(
    val mac: String,
    val hostname: String?,
    val ownerDisplayName: String,
    val ownerEmail: String,
    val note: String,
    val validUntil: Instant?,
)
