package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.core.value.AllowedMac
import java.time.Instant

interface FindAllowedMacPort {
    fun findByMac(mac: String): AllowedMac?

    fun findAll(): List<AllowedMac>

    fun findExpired(now: Instant): List<AllowedMac>
}
