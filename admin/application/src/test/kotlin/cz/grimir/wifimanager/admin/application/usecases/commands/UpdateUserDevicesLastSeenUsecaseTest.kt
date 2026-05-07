package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.command.UpdateUserDevicesLastSeenCommand
import cz.grimir.wifimanager.admin.application.command.handler.UpdateUserDevicesLastSeenUsecase
import cz.grimir.wifimanager.admin.application.port.FindUserDevicePort
import cz.grimir.wifimanager.admin.application.port.SaveUserDevicePort
import cz.grimir.wifimanager.admin.core.value.UserDevice
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceLastSeenChange
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
class UpdateUserDevicesLastSeenUsecaseTest {
    private val findUserDevicePort: FindUserDevicePort = mock()
    private val saveUserDevicePort: SaveUserDevicePort = mock()
    private val usecase = UpdateUserDevicesLastSeenUsecase(findUserDevicePort, saveUserDevicePort)

    @Test
    fun `updates last seen when device exists`() {
        val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val mac = "AA:BB:CC:DD:EE:FF"
        val seenAt = Instant.parse("2025-02-03T12:00:00Z")
        given(findUserDevicePort.findByUserIdAndMac(userId, mac)).willReturn(buildDevice(userId, mac))

        usecase.update(
            UpdateUserDevicesLastSeenCommand(
                listOf(NetworkUserDeviceLastSeenChange(userId, mac, seenAt)),
            ),
        )

        val captor = argumentCaptor<UserDevice>()
        verify(saveUserDevicePort).save(captor.capture())
        assertEquals(seenAt, captor.firstValue.lastSeenAt)
    }

    @Test
    fun `does nothing when device does not exist`() {
        val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val mac = "AA:BB:CC:DD:EE:FF"
        given(findUserDevicePort.findByUserIdAndMac(userId, mac)).willReturn(null)

        usecase.update(
            UpdateUserDevicesLastSeenCommand(
                listOf(
                    NetworkUserDeviceLastSeenChange(
                        userId,
                        mac,
                        Instant.parse("2025-02-03T12:00:00Z"),
                    ),
                ),
            ),
        )

        verifyNoInteractions(saveUserDevicePort)
    }

    @Test
    fun `skips update when reported timestamp is not newer`() {
        val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val mac = "AA:BB:CC:DD:EE:FF"
        given(findUserDevicePort.findByUserIdAndMac(userId, mac)).willReturn(buildDevice(userId, mac))

        usecase.update(
            UpdateUserDevicesLastSeenCommand(
                listOf(
                    NetworkUserDeviceLastSeenChange(
                        userId,
                        mac,
                        Instant.parse("2025-01-01T10:00:00Z"),
                    ),
                ),
            ),
        )

        verifyNoInteractions(saveUserDevicePort)
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
}
