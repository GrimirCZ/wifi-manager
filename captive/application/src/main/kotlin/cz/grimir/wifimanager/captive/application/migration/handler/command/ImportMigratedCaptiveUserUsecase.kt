package cz.grimir.wifimanager.captive.application.migration.handler.command

import cz.grimir.wifimanager.admin.application.migration.command.ImportAdminMigratedTicketsCommand
import cz.grimir.wifimanager.admin.application.migration.handler.command.ImportAdminMigratedTicketsUsecase
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.authorization.port.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.migration.model.ImportMigratedUserCommand
import cz.grimir.wifimanager.captive.application.migration.model.ImportedAuthorizedDevice
import cz.grimir.wifimanager.captive.application.migration.model.MigrationImportResult
import cz.grimir.wifimanager.captive.application.migration.port.MigratableUserDirectoryPort
import cz.grimir.wifimanager.captive.application.networkuser.handler.command.UpsertNetworkUserOnLoginUsecase
import cz.grimir.wifimanager.captive.application.networkuserdevice.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.captive.application.networkuserdevice.support.MacAddressUtils
import cz.grimir.wifimanager.captive.application.shared.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.ResolveUserCommand
import cz.grimir.wifimanager.shared.core.RoleMappingInput
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserDirectoryClient
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserProfileSnapshot
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ImportMigratedCaptiveUserUsecase(
    private val migratableUserDirectoryPort: MigratableUserDirectoryPort,
    private val userDirectoryClient: UserDirectoryClient,
    private val upsertNetworkUserOnLoginUsecase: UpsertNetworkUserOnLoginUsecase,
    private val importAdminMigratedTicketsUsecase: ImportAdminMigratedTicketsUsecase,
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort,
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort,
    private val captiveEventPublisher: CaptiveEventPublisher,
    private val timeProvider: TimeProvider,
) {
    @Transactional
    fun importUser(command: ImportMigratedUserCommand): MigrationImportResult {
        val now = timeProvider.get()
        val email = command.email.trim().lowercase()
        val directoryUser = migratableUserDirectoryPort.resolveByEmail(email) ?: throw MigrationUserNotFoundException(email)

        val resolvedIdentity =
            userDirectoryClient.resolveUser(
                ResolveUserCommand(
                    issuer = GOOGLE_ISSUER,
                    subject = directoryUser.subject,
                    profile =
                        UserProfileSnapshot(
                            displayName = directoryUser.displayName,
                            email = directoryUser.email,
                            pictureUrl = directoryUser.pictureUrl,
                        ),
                    roleMapping = RoleMappingInput(groups = directoryUser.groups),
                    loginAt = now,
                ),
            )

        val identity =
            UserIdentitySnapshot(
                userId = UserId(resolvedIdentity.userId),
                identityId = resolvedIdentity.identityId,
                displayName = resolvedIdentity.displayName,
                email = resolvedIdentity.email,
                pictureUrl = resolvedIdentity.pictureUrl,
                roles = resolvedIdentity.roles,
            )

        upsertNetworkUserOnLoginUsecase.upsert(identity, directoryUser.allowedDeviceCount)

        val activeTickets =
            command.tickets.filter { ticket ->
                ticket.lengthSeconds > 0 && ticket.start.plusSeconds(ticket.lengthSeconds).isAfter(now)
            }

        val importedTickets =
            importAdminMigratedTicketsUsecase.import(
                ImportAdminMigratedTicketsCommand(
                    user = identity,
                    tickets =
                        activeTickets.map { ticket ->
                            ImportAdminMigratedTicketsCommand.ImportedAdminTicket(
                                start = ticket.start,
                                lengthSeconds = ticket.lengthSeconds,
                                requireUserNameOnLogin = false,
                                authorizedDevices =
                                    ticket.authorizedDevices.map { device ->
                                        val mac = normalizeMac(device.mac)
                                        ImportAdminMigratedTicketsCommand.ImportedAdminAuthorizedDevice(
                                            mac = mac,
                                            displayName = device.displayName?.trim()?.ifBlank { null },
                                            deviceName = device.deviceName?.trim()?.ifBlank { null },
                                        )
                                    },
                            )
                        },
                ),
            )

        var importedDeviceCount = 0

        importedTickets.zip(activeTickets).forEach { (createdTicket, sourceTicket) ->
            val token =
                findAuthorizationTokenPort.findByTicketId(createdTicket.ticketId)
                    ?: error("Captive authorization token was not created for ticketId=${createdTicket.ticketId.id}")

            sourceTicket.authorizedDevices.forEach { importedDevice ->
                val mac = normalizeMac(importedDevice.mac)
                ensureMacOwnership(identity.userId, mac)

                val device =
                    Device(
                        mac = mac,
                        displayName = importedDevice.displayName?.trim()?.ifBlank { null },
                        deviceName = importedDevice.deviceName?.trim()?.ifBlank { null },
                    )

                token.authorizedDevices.removeIf { it.mac == mac }
                token.authorizeDevice(device)
                networkUserDeviceWritePort.save(
                    NetworkUserDevice(
                        userId = identity.userId,
                        mac = mac,
                        name = device.displayName ?: device.deviceName,
                        hostname = device.deviceName,
                        isRandomized = MacAddressUtils.isLocallyAdministered(mac),
                        authorizedAt = sourceTicket.start,
                        lastSeenAt = now,
                    ),
                )
                captiveEventPublisher.publish(
                    DeviceAuthorizedEvent(
                        ticketId = createdTicket.ticketId,
                        device =
                            DeviceAuthorizedEvent.Device(
                                macAddress = mac,
                                displayName = device.displayName,
                                deviceName = device.deviceName,
                            ),
                        authorizedAt = sourceTicket.start,
                    ),
                )
                importedDeviceCount++
            }

            modifyAuthorizationTokenPort.save(token)
        }

        return MigrationImportResult(
            userId = identity.userId.id,
            identityId = identity.identityId,
            email = identity.email,
            importedTicketCount = importedTickets.size,
            importedDeviceCount = importedDeviceCount,
        )
    }

    private fun ensureMacOwnership(
        userId: UserId,
        mac: String,
    ) {
        val existing = networkUserDeviceReadPort.findByMac(mac) ?: return
        if (existing.userId != userId) {
            throw MigrationMacOwnershipConflictException(mac, existing.userId.id)
        }
    }

    private fun normalizeMac(mac: String): String {
        val cleaned = mac.trim().replace("-", ":").uppercase()
        val parts = cleaned.split(":")
        require(parts.size == 6 && parts.all { it.length == 2 && it.all { ch -> ch.isDigit() || ch in 'A'..'F' } }) {
            "Invalid MAC address: $mac"
        }
        return parts.joinToString(":")
    }

    companion object {
        const val GOOGLE_ISSUER: String = "https://accounts.google.com"
    }
}

class MigrationUserNotFoundException(
    val email: String,
) : RuntimeException("No Google directory user found for $email")

class MigrationMacOwnershipConflictException(
    val mac: String,
    val ownerUserId: UUID,
) : RuntimeException("MAC $mac is already assigned to user $ownerUserId")
