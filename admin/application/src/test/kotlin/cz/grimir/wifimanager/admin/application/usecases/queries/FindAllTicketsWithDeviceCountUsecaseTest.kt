package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.admin.application.port.FindTicketPort
import cz.grimir.wifimanager.admin.application.query.FindAllTicketsWithDeviceCountQuery
import cz.grimir.wifimanager.admin.application.query.handler.FindAllTicketsWithDeviceCountUsecase
import cz.grimir.wifimanager.admin.application.query.model.TicketWithDeviceCount
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.Instant
import java.util.UUID

class FindAllTicketsWithDeviceCountUsecaseTest {
    private val findTicketPort: FindTicketPort = mock()
    private val usecase = FindAllTicketsWithDeviceCountUsecase(findTicketPort)

    @Test
    fun `returns all tickets with device counts from port`() {
        val ticket =
            Ticket(
                id = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                accessCode = "ABC123",
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                validUntil = Instant.parse("2025-01-01T10:00:00Z"),
                authorId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
            )
        val tickets = listOf(TicketWithDeviceCount(ticket, 2))
        given(findTicketPort.findAllWithDeviceCount()).willReturn(tickets)

        assertEquals(tickets, usecase.find(FindAllTicketsWithDeviceCountQuery))
    }
}
