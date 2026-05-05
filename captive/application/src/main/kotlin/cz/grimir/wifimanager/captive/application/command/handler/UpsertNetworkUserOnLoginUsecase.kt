package cz.grimir.wifimanager.captive.application.command.handler

import cz.grimir.wifimanager.captive.application.command.UpsertNetworkUserOnLoginCommand
import cz.grimir.wifimanager.captive.application.port.NetworkUserReadPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserWritePort
import cz.grimir.wifimanager.captive.application.query.model.NetworkUser
import cz.grimir.wifimanager.shared.core.TimeProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpsertNetworkUserOnLoginUsecase(
    private val networkUserReadPort: NetworkUserReadPort,
    private val networkUserWritePort: NetworkUserWritePort,
    private val timeProvider: TimeProvider,
) {
    @Transactional
    fun upsert(command: UpsertNetworkUserOnLoginCommand): NetworkUser {
        val loginAt = timeProvider.get()
        val existing = networkUserReadPort.findByUserId(command.identity.userId)
        val user =
            if (existing == null) {
                NetworkUser(
                    userId = command.identity.userId,
                    identityId = command.identity.identityId,
                    allowedDeviceCount = command.allowedDeviceCount,
                    adminOverrideLimit = null,
                    createdAt = loginAt,
                    updatedAt = loginAt,
                    lastLoginAt = loginAt,
                )
            } else {
                existing.copy(
                    allowedDeviceCount = command.allowedDeviceCount,
                    updatedAt = loginAt,
                    lastLoginAt = loginAt,
                )
            }
        return networkUserWritePort.save(user)
    }
}
