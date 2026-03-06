package cz.grimir.wifimanager.admin.application.allowedmac.port

interface DeleteAllowedMacPort {
    fun deleteByMac(mac: String)
}
