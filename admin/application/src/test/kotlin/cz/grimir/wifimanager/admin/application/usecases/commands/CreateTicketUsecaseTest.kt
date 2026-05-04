package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.port.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.command.CreateTicketCommand
import cz.grimir.wifimanager.admin.application.command.handler.CreateTicketUsecase
import cz.grimir.wifimanager.admin.application.port.FindTicketPort
import cz.grimir.wifimanager.admin.application.port.SaveTicketPort
import cz.grimir.wifimanager.admin.application.support.AccessCodeGenerator
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.events.TicketCreatedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import java.security.SecureRandom
import java.time.Duration
import java.util.UUID

class CreateTicketUsecaseTest {
    private val saveTicketPort: SaveTicketPort = mock()
    private val findTicketPort: FindTicketPort = mock()
    private val eventPublisher: AdminEventPublisher = mock()
    private val accessCodeGenerator = AccessCodeGenerator(SecureRandom.getInstance("SHA1PRNG").apply { setSeed(1L) })

    private val usecase =
        CreateTicketUsecase(
            saveTicketPort = saveTicketPort,
            findTicketPort = findTicketPort,
            eventPublisher = eventPublisher,
            accessCodeGenerator = accessCodeGenerator,
        )

    @Test
    fun `creates ticket with require name disabled by default`() {
        val user = testUser()
        given(findTicketPort.findByAuthorId(user.userId.id)).willReturn(emptyList())

        usecase.create(
            CreateTicketCommand(
                accessCode = null,
                duration = Duration.ofMinutes(45),
                user = user,
                requireUserNameOnLogin = false,
            ),
        )

        val ticketCaptor = argumentCaptor<Ticket>()
        verify(saveTicketPort).save(ticketCaptor.capture())
        assertFalse(ticketCaptor.firstValue.requireUserNameOnLogin)
        assertEquals(6, ticketCaptor.firstValue.accessCode.length)

        val eventCaptor = argumentCaptor<TicketCreatedEvent>()
        verify(eventPublisher).publish(eventCaptor.capture())
        assertFalse(eventCaptor.firstValue.requireUserNameOnLogin)
        assertEquals(6, eventCaptor.firstValue.accessCode.length)
    }

    @Test
    fun `creates ticket with require name enabled`() {
        val user = testUser()
        given(findTicketPort.findByAuthorId(user.userId.id)).willReturn(emptyList())

        usecase.create(
            CreateTicketCommand(
                accessCode = null,
                duration = Duration.ofMinutes(45),
                user = user,
                requireUserNameOnLogin = true,
            ),
        )

        val ticketCaptor = argumentCaptor<Ticket>()
        verify(saveTicketPort).save(ticketCaptor.capture())
        assertTrue(ticketCaptor.firstValue.requireUserNameOnLogin)

        val eventCaptor = argumentCaptor<TicketCreatedEvent>()
        verify(eventPublisher).publish(eventCaptor.capture())
        assertTrue(eventCaptor.firstValue.requireUserNameOnLogin)
    }

    @Test
    fun `staff can create standard two hour ticket`() {
        val user = testUser(roles = setOf(UserRole.WIFI_STAFF))
        given(findTicketPort.findByAuthorId(user.userId.id)).willReturn(emptyList())

        usecase.create(
            CreateTicketCommand(
                accessCode = null,
                duration = Duration.ofMinutes(120),
                user = user,
                requireUserNameOnLogin = false,
            ),
        )

        val ticketCaptor = argumentCaptor<Ticket>()
        verify(saveTicketPort).save(ticketCaptor.capture())
        assertEquals(120, Duration.between(ticketCaptor.firstValue.createdAt, ticketCaptor.firstValue.validUntil).toMinutes())
    }

    @Test
    fun `staff cannot create extended four hour ticket`() {
        val user = testUser(roles = setOf(UserRole.WIFI_STAFF))

        assertThrows(IllegalArgumentException::class.java) {
            usecase.create(
                CreateTicketCommand(
                    accessCode = null,
                    duration = Duration.ofMinutes(240),
                    user = user,
                    requireUserNameOnLogin = false,
                ),
            )
        }

        verify(saveTicketPort, never()).save(org.mockito.kotlin.any())
    }

    @Test
    fun `staff cannot create extended seven day ticket`() {
        val user = testUser(roles = setOf(UserRole.WIFI_STAFF))

        assertThrows(IllegalArgumentException::class.java) {
            usecase.create(
                CreateTicketCommand(
                    accessCode = null,
                    duration = Duration.ofMinutes(10080),
                    user = user,
                    requireUserNameOnLogin = false,
                ),
            )
        }

        verify(saveTicketPort, never()).save(org.mockito.kotlin.any())
    }

    @Test
    fun `admin can create extended seven day ticket`() {
        val user = testUser(roles = setOf(UserRole.WIFI_ADMIN))
        given(findTicketPort.findByAuthorId(user.userId.id)).willReturn(emptyList())

        usecase.create(
            CreateTicketCommand(
                accessCode = null,
                duration = Duration.ofMinutes(10080),
                user = user,
                requireUserNameOnLogin = false,
            ),
        )

        val ticketCaptor = argumentCaptor<Ticket>()
        verify(saveTicketPort).save(ticketCaptor.capture())
        assertEquals(10080, Duration.between(ticketCaptor.firstValue.createdAt, ticketCaptor.firstValue.validUntil).toMinutes())
    }

    @Test
    fun `rejects unsupported duration`() {
        val user = testUser(roles = setOf(UserRole.WIFI_ADMIN))

        assertThrows(IllegalArgumentException::class.java) {
            usecase.create(
                CreateTicketCommand(
                    accessCode = null,
                    duration = Duration.ofMinutes(121),
                    user = user,
                    requireUserNameOnLogin = false,
                ),
            )
        }

        verify(saveTicketPort, never()).save(org.mockito.kotlin.any())
    }

    @Test
    fun `rejects duration above seven days`() {
        val user = testUser(roles = setOf(UserRole.WIFI_ADMIN))

        assertThrows(IllegalArgumentException::class.java) {
            usecase.create(
                CreateTicketCommand(
                    accessCode = null,
                    duration = Duration.ofMinutes(10081),
                    user = user,
                    requireUserNameOnLogin = false,
                ),
            )
        }

        verify(saveTicketPort, never()).save(org.mockito.kotlin.any())
    }

    private fun testUser(roles: Set<UserRole> = emptySet()): UserIdentitySnapshot =
        UserIdentitySnapshot(
            userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000100")),
            identityId = UUID.fromString("00000000-0000-0000-0000-000000000101"),
            displayName = "Test User",
            email = "user@example.com",
            pictureUrl = null,
            roles = roles,
        )
}
