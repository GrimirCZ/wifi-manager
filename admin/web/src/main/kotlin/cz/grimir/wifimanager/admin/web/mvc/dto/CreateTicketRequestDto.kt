package cz.grimir.wifimanager.admin.web.mvc.dto

data class CreateTicketRequestDto(
    val validityMinutes: Int = 45,
)
