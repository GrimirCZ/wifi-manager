package cz.grimir.wifimanager.admin.web.mvc.dto

data class NetworkClientViewDto(
    val macAddress: String,
    val ipAddresses: List<String>,
    val hostname: String?,
    val allowed: Boolean,
    val isLocalAdmin: Boolean,
)
