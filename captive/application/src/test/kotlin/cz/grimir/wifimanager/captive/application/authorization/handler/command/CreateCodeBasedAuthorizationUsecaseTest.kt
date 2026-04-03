package cz.grimir.wifimanager.captive.application.authorization.handler.command

import cz.grimir.wifimanager.captive.application.authorization.command.CreateCodeBasedAuthorizationCommand
import cz.grimir.wifimanager.captive.application.authorization.port.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import java.time.Instant
import java.util.UUID

class CreateCodeBasedAuthorizationUsecaseTest {
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort = mock()

    private val usecase =
        CreateCodeBasedAuthorizationUsecase(
            modifyAuthorizationTokenPort = modifyAuthorizationTokenPort,
        )

    @Test
    fun `stores require user name flag in created token`() {
        usecase.create(
            CreateCodeBasedAuthorizationCommand(
                ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000200")),
                accessCode = "ABCDEFGH",
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                validUntil = Instant.parse("2025-01-01T10:00:00Z"),
                userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000201")),
                requireUserNameOnLogin = true,
            ),
        )

        val tokenCaptor = argumentCaptor<AuthorizationToken>()
        verify(modifyAuthorizationTokenPort).save(tokenCaptor.capture())
        assertTrue(tokenCaptor.firstValue.requireUserNameOnLogin)
    }
}
