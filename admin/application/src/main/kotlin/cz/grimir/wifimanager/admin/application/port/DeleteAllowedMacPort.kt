package cz.grimir.wifimanager.admin.application.port

interface DeleteAllowedMacPort {
    fun deleteByMac(mac: String)
}
