package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.application.model.User

interface SaveUserPort {
    fun save(user: User): User
}
