package cz.grimir.wifimanager.admin.application.usecases.scheduler

import cz.grimir.wifimanager.admin.application.commands.ExpireTicketsCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import cz.grimir.wifimanager.admin.application.usecases.commands.ExpireExpiredTicketsUsecase
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.events.TicketEndedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ExpireExpiredTicketsUsecaseTest {
    private val findTicketPort: FindTicketPort = mock()

    private val saveTicketPort: SaveTicketPort = mock()

    private val eventPublisher: AdminEventPublisher = mock()

    private val usecase =
        ExpireExpiredTicketsUsecase(
            findTicketPort,
            saveTicketPort,
            eventPublisher,
        )

    @Test
    fun `expires tickets and publishes events`() {
        val now = Instant.parse("2025-01-01T10:00:00Z")
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val expiredTicket =
            Ticket(
                id = ticketId,
                accessCode = "ABC123",
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                validUntil = Instant.parse("2025-01-01T09:30:00Z"),
                wasCanceled = false,
                authorId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
            )

        given(findTicketPort.findExpired(now)).willReturn(listOf(expiredTicket))

        val expiredCount = usecase.expireExpiredTickets(ExpireTicketsCommand(now))

        assertEquals(1, expiredCount)

        val savedTicketCaptor = argumentCaptor<Ticket>()
        verify(saveTicketPort).save(savedTicketCaptor.capture())
        assertTrue(savedTicketCaptor.firstValue.wasCanceled)

        val endedEventCaptor = argumentCaptor<TicketEndedEvent>()
        verify(eventPublisher).publish(endedEventCaptor.capture())
        assertEquals(ticketId, endedEventCaptor.firstValue.ticketId)
        assertEquals(TicketEndedEvent.Reason.EXPIRED, endedEventCaptor.firstValue.reason)
        assertEquals(now, endedEventCaptor.firstValue.endedAt)
    }
}
