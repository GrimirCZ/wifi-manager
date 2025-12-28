package cz.grimir.wifimanager.admin.persistence.projection

import cz.grimir.wifimanager.admin.persistence.entity.AdminTicketEntity

data class AdminTicketWithDeviceCountProjection(
    val ticket: AdminTicketEntity,
    val deviceCount: Long,
)
