package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.admin.application.identity.handler.query.FindUserIdentityByUserIdUsecase
import cz.grimir.wifimanager.admin.application.identity.model.UserIdentity
import cz.grimir.wifimanager.admin.application.identity.port.FindUserIdentityPort
import cz.grimir.wifimanager.admin.application.identity.query.FindUserIdentityByUserIdQuery
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.Instant
import java.util.UUID

class FindUserIdentityByUserIdUsecaseTest {
    private val findUserIdentityPort: FindUserIdentityPort = mock()
    private val usecase = FindUserIdentityByUserIdUsecase(findUserIdentityPort)

    @Test
    fun `returns identity by user id from port`() {
        val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
        val identity =
            UserIdentity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000011"),
                userId = userId,
                issuer = "issuer",
                subject = "subject",
                email = "owner@example.com",
                displayName = "Owner Name",
                pictureUrl = null,
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                roles = setOf(UserRole.WIFI_STAFF),
            )
        given(findUserIdentityPort.findByUserId(userId)).willReturn(identity)

        assertEquals(identity, usecase.find(FindUserIdentityByUserIdQuery(userId)))
    }
}
