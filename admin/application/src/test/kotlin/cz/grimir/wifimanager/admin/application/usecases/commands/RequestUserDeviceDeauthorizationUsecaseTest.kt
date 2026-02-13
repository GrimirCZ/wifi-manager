package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.RequestUserDeviceDeauthorizationCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindUserDevicePort
import cz.grimir.wifimanager.admin.core.value.UserDevice
import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceDeauthorizeRequestedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RequestUserDeviceDeauthorizationUsecaseTest {
    private val findUserDevicePort: FindUserDevicePort = mock()
    private val adminEventPublisher: AdminEventPublisher = mock()
    private val fixedInstant = Instant.parse("2025-02-01T12:34:56Z")
    private val timeProvider = TimeProvider { fixedInstant }

    private val usecase =
        RequestUserDeviceDeauthorizationUsecase(
            findUserDevicePort = findUserDevicePort,
            adminEventPublisher = adminEventPublisher,
            timeProvider = timeProvider,
        )

    @Test
    fun `publishes event when device exists`() {
        val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val deviceMac = "AA:BB:CC:DD:EE:FF"
        val requester = buildUser(UUID.fromString("00000000-0000-0000-0000-000000000002"))
        given(findUserDevicePort.findByUserIdAndMac(userId, deviceMac)).willReturn(buildDevice(userId, deviceMac))

        usecase.request(
            RequestUserDeviceDeauthorizationCommand(
                userId = userId,
                deviceMac = deviceMac,
                requestedBy = requester,
            ),
        )

        val eventCaptor = argumentCaptor<NetworkUserDeviceDeauthorizeRequestedEvent>()
        verify(adminEventPublisher).publish(eventCaptor.capture())
        assertEquals(userId, eventCaptor.firstValue.userId)
        assertEquals(deviceMac, eventCaptor.firstValue.deviceMac)
        assertEquals(requester.userId, eventCaptor.firstValue.requestedByUserId)
        assertEquals(fixedInstant, eventCaptor.firstValue.requestedAt)
    }

    @Test
    fun `does nothing when device does not exist`() {
        val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val deviceMac = "AA:BB:CC:DD:EE:FF"
        val requester = buildUser(UUID.fromString("00000000-0000-0000-0000-000000000002"))
        given(findUserDevicePort.findByUserIdAndMac(userId, deviceMac)).willReturn(null)

        usecase.request(
            RequestUserDeviceDeauthorizationCommand(
                userId = userId,
                deviceMac = deviceMac,
                requestedBy = requester,
            ),
        )

        verifyNoInteractions(adminEventPublisher)
    }

    private fun buildDevice(
        userId: UserId,
        mac: String,
    ) = UserDevice(
        userId = userId,
        mac = mac,
        name = "Phone",
        hostname = "phone-host",
        isRandomized = false,
        authorizedAt = Instant.parse("2025-01-01T10:00:00Z"),
        lastSeenAt = Instant.parse("2025-01-01T11:00:00Z"),
    )

    private fun buildUser(userId: UUID): UserIdentitySnapshot =
        UserIdentitySnapshot(
            userId = UserId(userId),
            identityId = UUID.randomUUID(),
            displayName = "user",
            email = "user@example.com",
            pictureUrl = null,
            roles = setOf(UserRole.WIFI_STAFF),
        )
}
