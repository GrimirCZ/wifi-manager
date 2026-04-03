package cz.grimir.wifimanager.captive.application.migration.handler.command

import cz.grimir.wifimanager.admin.application.migration.command.ImportAdminMigratedTicketsCommand
import cz.grimir.wifimanager.admin.application.migration.handler.command.ImportAdminMigratedTicketsUsecase
import cz.grimir.wifimanager.admin.application.migration.model.ImportedAdminTicketResult
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.authorization.port.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.migration.model.ImportMigratedUserCommand
import cz.grimir.wifimanager.captive.application.migration.model.ImportedActiveTicket
import cz.grimir.wifimanager.captive.application.migration.model.ImportedAuthorizedDevice
import cz.grimir.wifimanager.captive.application.migration.model.MigratableDirectoryUser
import cz.grimir.wifimanager.captive.application.migration.port.MigratableUserDirectoryPort
import cz.grimir.wifimanager.captive.application.networkuser.handler.command.UpsertNetworkUserOnLoginUsecase
import cz.grimir.wifimanager.captive.application.networkuser.model.NetworkUser
import cz.grimir.wifimanager.captive.application.networkuserdevice.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.captive.application.shared.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.shared.core.ResolveUserCommand
import cz.grimir.wifimanager.shared.core.ResolveUserResult
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserDirectoryClient
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class ImportMigratedCaptiveUserUsecaseTest {
    private val migratableUserDirectoryPort: MigratableUserDirectoryPort = mock()
    private val userDirectoryClient: UserDirectoryClient = mock()
    private val upsertNetworkUserOnLoginUsecase: UpsertNetworkUserOnLoginUsecase = mock()
    private val importAdminMigratedTicketsUsecase: ImportAdminMigratedTicketsUsecase = mock()
    private val tokenStore = InMemoryTokenStore()
    private val deviceStore = InMemoryNetworkUserDeviceStore()
    private val captiveEventPublisher: CaptiveEventPublisher = mock()
    private val timeProvider = TimeProvider { NOW }

    private val usecase =
        ImportMigratedCaptiveUserUsecase(
            migratableUserDirectoryPort = migratableUserDirectoryPort,
            userDirectoryClient = userDirectoryClient,
            upsertNetworkUserOnLoginUsecase = upsertNetworkUserOnLoginUsecase,
            importAdminMigratedTicketsUsecase = importAdminMigratedTicketsUsecase,
            findAuthorizationTokenPort = tokenStore,
            modifyAuthorizationTokenPort = tokenStore,
            networkUserDeviceReadPort = deviceStore,
            networkUserDeviceWritePort = deviceStore,
            captiveEventPublisher = captiveEventPublisher,
            timeProvider = timeProvider,
        )

    @Test
    fun `imports active tickets through admin and updates captive token devices`() {
        stubResolvedUser()
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000333"))
        tokenStore.save(
            AuthorizationToken(
                id = ticketId,
                accessCode = "ABCDEFGH",
                validUntil = NOW.plusSeconds(3600),
                requireUserNameOnLogin = false,
                authorizedDevices = mutableListOf(),
                kickedMacAddresses = mutableSetOf(),
            ),
        )
        whenever(importAdminMigratedTicketsUsecase.import(org.mockito.kotlin.any())).thenReturn(
            listOf(
                ImportedAdminTicketResult(
                    ticketId = ticketId,
                    accessCode = "ABCDEFGH",
                    createdAt = NOW.minusSeconds(60),
                    validUntil = NOW.plusSeconds(3600),
                ),
            ),
        )

        val result = usecase.importUser(activeImportCommand())

        assertEquals(1, result.importedTicketCount)
        assertEquals(1, result.importedDeviceCount)

        verify(upsertNetworkUserOnLoginUsecase).upsert(
            org.mockito.kotlin.check { assertEquals(USER_ID, it.userId.id) },
            eq(7),
        )

        val importCaptor = argumentCaptor<ImportAdminMigratedTicketsCommand>()
        verify(importAdminMigratedTicketsUsecase).import(importCaptor.capture())
        assertEquals(1, importCaptor.firstValue.tickets.size)

        val token = tokenStore.findByTicketId(ticketId)
        assertEquals(1, token?.authorizedDevices?.size)
        assertEquals("AA:BB:CC:DD:EE:FF", token?.authorizedDevices?.single()?.mac)

        val storedDevice = deviceStore.findByMac("AA:BB:CC:DD:EE:FF")
        assertEquals("John phone", storedDevice?.name)

        val eventCaptor = argumentCaptor<DeviceAuthorizedEvent>()
        verify(captiveEventPublisher).publish(eventCaptor.capture())
        assertEquals(ticketId, eventCaptor.firstValue.ticketId)
    }

    @Test
    fun `skips expired tickets before calling admin import`() {
        stubResolvedUser()

        val result =
            usecase.importUser(
                ImportMigratedUserCommand(
                    email = "user@example.com",
                    tickets =
                        listOf(
                            ImportedActiveTicket(
                                start = NOW.minusSeconds(7200),
                                lengthSeconds = 60,
                                authorizedDevices =
                                    listOf(
                                        ImportedAuthorizedDevice(
                                            mac = "AA:BB:CC:DD:EE:FF",
                                            deviceName = "iPhone",
                                            displayName = "John phone",
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        assertEquals(0, result.importedTicketCount)
        assertEquals(0, result.importedDeviceCount)
        verify(importAdminMigratedTicketsUsecase).import(
            org.mockito.kotlin.check { assertEquals(0, it.tickets.size) },
        )
    }

    @Test
    fun `fails when mac belongs to another user`() {
        stubResolvedUser()
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000333"))
        tokenStore.save(
            AuthorizationToken(
                id = ticketId,
                accessCode = "ABCDEFGH",
                validUntil = NOW.plusSeconds(3600),
                requireUserNameOnLogin = false,
                authorizedDevices = mutableListOf(),
                kickedMacAddresses = mutableSetOf(),
            ),
        )
        whenever(importAdminMigratedTicketsUsecase.import(org.mockito.kotlin.any())).thenReturn(
            listOf(
                ImportedAdminTicketResult(
                    ticketId = ticketId,
                    accessCode = "ABCDEFGH",
                    createdAt = NOW.minusSeconds(60),
                    validUntil = NOW.plusSeconds(3600),
                ),
            ),
        )
        deviceStore.save(
            NetworkUserDevice(
                userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000999")),
                mac = "AA:BB:CC:DD:EE:FF",
                name = "Owned elsewhere",
                hostname = "host",
                isRandomized = false,
                authorizedAt = NOW,
                lastSeenAt = NOW,
            ),
        )

        assertThrows(MigrationMacOwnershipConflictException::class.java) {
            usecase.importUser(activeImportCommand())
        }
    }

    private fun stubResolvedUser() {
        whenever(migratableUserDirectoryPort.resolveByEmail("user@example.com")).thenReturn(
            MigratableDirectoryUser(
                subject = "google-subject-1",
                email = "user@example.com",
                displayName = "User Example",
                pictureUrl = null,
                groups = setOf("wifi-users@example.com"),
                allowedDeviceCount = 7,
            ),
        )
        whenever(userDirectoryClient.resolveUser(org.mockito.kotlin.any())).thenReturn(
            ResolveUserResult(
                userId = USER_ID,
                identityId = IDENTITY_ID,
                displayName = "User Example",
                email = "user@example.com",
                pictureUrl = null,
                roles = setOf(UserRole.WIFI_STAFF),
            ),
        )
        whenever(upsertNetworkUserOnLoginUsecase.upsert(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(
            NetworkUser(
                userId = UserId(USER_ID),
                identityId = IDENTITY_ID,
                allowedDeviceCount = 7,
                adminOverrideLimit = null,
                createdAt = NOW,
                updatedAt = NOW,
                lastLoginAt = NOW,
            ),
        )
    }

    private fun activeImportCommand(): ImportMigratedUserCommand =
        ImportMigratedUserCommand(
            email = "user@example.com",
            tickets =
                listOf(
                    ImportedActiveTicket(
                        start = NOW.minusSeconds(60),
                        lengthSeconds = 3600,
                        authorizedDevices =
                            listOf(
                                ImportedAuthorizedDevice(
                                    mac = "aa-bb-cc-dd-ee-ff",
                                    deviceName = "iPhone",
                                    displayName = "John phone",
                                ),
                            ),
                    ),
                ),
        )

    private class InMemoryTokenStore : FindAuthorizationTokenPort, ModifyAuthorizationTokenPort {
        private val tokens = linkedMapOf<UUID, AuthorizationToken>()

        override fun findByTicketId(ticketId: TicketId): AuthorizationToken? = tokens[ticketId.id]

        override fun findByAccessCode(accessCode: String): AuthorizationToken? = tokens.values.firstOrNull { it.accessCode == accessCode }

        override fun findByAuthorizedDeviceMac(macAddress: String): AuthorizationToken? =
            tokens.values.firstOrNull { token -> token.authorizedDevices.any { it.mac == macAddress } }

        override fun findAllAuthorizedDeviceMacs(): List<String> =
            tokens.values.flatMap { token -> token.authorizedDevices.map(Device::mac) }

        override fun save(token: AuthorizationToken) {
            tokens[token.id.id] = token
        }

        override fun deleteByTicketId(ticketId: TicketId) {
            tokens.remove(ticketId.id)
        }
    }

    private class InMemoryNetworkUserDeviceStore : NetworkUserDeviceReadPort, NetworkUserDeviceWritePort {
        private val devices = linkedMapOf<String, NetworkUserDevice>()

        override fun findByMac(mac: String): NetworkUserDevice? = devices[mac]

        override fun findByUserId(userId: UserId): List<NetworkUserDevice> = devices.values.filter { it.userId == userId }

        override fun countByUserId(userId: UserId): Long = findByUserId(userId).size.toLong()

        override fun save(device: NetworkUserDevice) {
            devices[device.mac] = device
        }

        override fun delete(
            userId: UserId,
            mac: String,
        ) {
            if (devices[mac]?.userId == userId) {
                devices.remove(mac)
            }
        }

        override fun touchDevice(
            userId: UserId,
            mac: String,
        ) {}
    }

    companion object {
        private val NOW: Instant = Instant.parse("2026-04-03T12:00:00Z")
        private val USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000111")
        private val IDENTITY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000222")
    }
}
