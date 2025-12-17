package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.application.model.User
import cz.grimir.wifimanager.shared.core.UserId

interface FindUserPort {
    fun findById(id: UserId): User?
}
