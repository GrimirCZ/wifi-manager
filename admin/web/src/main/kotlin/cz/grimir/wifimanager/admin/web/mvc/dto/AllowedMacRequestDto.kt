package cz.grimir.wifimanager.admin.web.mvc.dto

class AllowedMacRequestDto(
    var mac: String = "",
    var hostname: String? = null,
    var note: String = "",
    var validityMinutes: Long? = 0,
)
