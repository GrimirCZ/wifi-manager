package cz.grimir.wifimanager.admin.web.mvc

import cz.grimir.wifimanager.admin.application.identity.handler.query.FindUserIdentityByUserIdUsecase
import cz.grimir.wifimanager.admin.application.identity.model.UserIdentity
import cz.grimir.wifimanager.admin.application.identity.query.FindUserIdentityByUserIdQuery
import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.ui.ModelMap
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

class AdminUserLookupControllerTest {
    private val findUserIdentityByUserIdUsecase: FindUserIdentityByUserIdUsecase = mock()
    private val controller = AdminUserLookupController(findUserIdentityByUserIdUsecase)

    @Test
    fun `rejects non admin owner display name lookup`() {
        val exception =
            assertThrows(ResponseStatusException::class.java) {
                controller.displayName(staffUser(), UUID.fromString("00000000-0000-0000-0000-000000000010"), ModelMap())
            }

        assertEquals(403, exception.statusCode.value())
        verifyNoInteractions(findUserIdentityByUserIdUsecase)
    }

    @Test
    fun `returns display name fragment for admin`() {
        val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
        given(findUserIdentityByUserIdUsecase.find(FindUserIdentityByUserIdQuery(userId))).willReturn(
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
            ),
        )

        val model = ModelMap()
        val view = controller.displayName(adminUser(), userId.id, model)

        assertEquals("admin/fragments/user-display-name :: userDisplayName", view)
        assertEquals("Owner Name", model["displayName"])
        assertEquals(userId.id, model["userId"])
    }

    private fun staffUser(): UserIdentitySnapshot =
        user(UUID.fromString("00000000-0000-0000-0000-000000000020"), setOf(UserRole.WIFI_STAFF))

    private fun adminUser(): UserIdentitySnapshot =
        user(UUID.fromString("00000000-0000-0000-0000-000000000030"), setOf(UserRole.WIFI_ADMIN))

    private fun user(
        userId: UUID,
        roles: Set<UserRole>,
    ): UserIdentitySnapshot =
        UserIdentitySnapshot(
            userId = UserId(userId),
            identityId = UUID.randomUUID(),
            displayName = "User",
            email = "user@example.com",
            pictureUrl = null,
            roles = roles,
        )
}
