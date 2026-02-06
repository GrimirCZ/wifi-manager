package cz.grimir.wifimanager.admin.application.ports

interface DeleteAllowedMacPort {
    fun deleteByMac(mac: String)
}
