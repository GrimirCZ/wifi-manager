package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.ports.DeleteUserDevicePort
import cz.grimir.wifimanager.shared.core.UserId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RemoveUserDeviceUsecaseTest {
    private val deleteUserDevicePort: DeleteUserDevicePort = mock()
    private val usecase = RemoveUserDeviceUsecase(deleteUserDevicePort)

    @Test
    fun `deletes device`() {
        val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val mac = "AA:BB:CC:DD:EE:FF"

        usecase.remove(userId, mac)

        verify(deleteUserDevicePort).delete(userId, mac)
    }
}
