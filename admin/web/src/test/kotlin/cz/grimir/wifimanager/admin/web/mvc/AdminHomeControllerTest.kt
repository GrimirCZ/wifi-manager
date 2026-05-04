package cz.grimir.wifimanager.admin.web.mvc

import cz.grimir.wifimanager.admin.application.command.CancelTicketCommand
import cz.grimir.wifimanager.admin.application.command.handler.CancelTicketUsecase
import cz.grimir.wifimanager.admin.application.command.handler.CreateTicketUsecase
import cz.grimir.wifimanager.admin.application.command.handler.KickClientUsecase
import cz.grimir.wifimanager.admin.application.query.handler.CountAuthorizedDevicesByTicketIdUsecase
import cz.grimir.wifimanager.admin.application.query.handler.FindAllTicketsWithDeviceCountUsecase
import cz.grimir.wifimanager.admin.application.query.handler.FindAuthorizedDevicesByTicketIdUsecase
import cz.grimir.wifimanager.admin.application.query.handler.FindTicketByIdUsecase
import cz.grimir.wifimanager.admin.application.query.handler.FindTicketsByAuthorIdWithDeviceCountUsecase
import cz.grimir.wifimanager.admin.application.query.model.TicketWithDeviceCount
import cz.grimir.wifimanager.admin.application.query.FindTicketByIdQuery
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.web.AdminWifiProperties
import cz.grimir.wifimanager.shared.application.captive.CaptivePortalApiProperties
import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.ui.ModelMap
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.Instant
import java.util.UUID

class AdminHomeControllerTest {
    private val findTicketsByAuthorIdWithDeviceCountUsecase: FindTicketsByAuthorIdWithDeviceCountUsecase = mock()
    private val findAllTicketsWithDeviceCountUsecase: FindAllTicketsWithDeviceCountUsecase = mock()
    private val cancelTicketUsecase: CancelTicketUsecase = mock()
    private val findTicketByIdUsecase: FindTicketByIdUsecase = mock()
    private val controller =
        AdminHomeController(
            createTicketUsecase = mock(CreateTicketUsecase::class.java),
            cancelTicketUsecase = cancelTicketUsecase,
            findTicketByIdUsecase = findTicketByIdUsecase,
            findTicketsByAuthorIdWithDeviceCountUsecase = findTicketsByAuthorIdWithDeviceCountUsecase,
            findAllTicketsWithDeviceCountUsecase = findAllTicketsWithDeviceCountUsecase,
            findAuthorizedDevicesByTicketIdUsecase = mock(FindAuthorizedDevicesByTicketIdUsecase::class.java),
            countAuthorizedDevicesByTicketIdUsecase = mock(CountAuthorizedDevicesByTicketIdUsecase::class.java),
            kickClientUsecase = mock(KickClientUsecase::class.java),
            wifiProperties = AdminWifiProperties("Test WiFi"),
            captivePortalApiProperties = CaptivePortalApiProperties("https://portal.example"),
        )

    @Test
    fun `staff cannot open all ticket scope`() {
        val exception =
            assertThrows(ResponseStatusException::class.java) {
                controller.index(staffUser(), "all", htmxRequest(), ModelMap())
            }

        assertEquals(403, exception.statusCode.value())
    }

    @Test
    fun `admin all ticket scope loads all active tickets and enables owner lookup`() {
        val admin = adminUser()
        val activeTicket =
            TicketWithDeviceCount(
                ticket =
                    Ticket(
                        id = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000030")),
                        accessCode = "ABC123",
                        createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                        validUntil = Instant.now().plusSeconds(3600),
                        authorId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000031")),
                    ),
                deviceCount = 1,
            )
        given(findAllTicketsWithDeviceCountUsecase.find()).willReturn(listOf(activeTicket))

        val model = ModelMap()
        val view = controller.index(admin, "all", htmxRequest(), model)

        assertEquals("admin/index", view)
        assertEquals("all", model["ticketScope"])
        assertEquals(true, model["showTicketOwnerLookup"])
        assertEquals(listOf(activeTicket), model["activeTickets"])
    }

    @Test
    fun `admin can end another users ticket`() {
        val admin = adminUser()
        val ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000040"))
        val ticket =
            Ticket(
                id = ticketId,
                accessCode = "ABC123",
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                validUntil = Instant.now().plusSeconds(3600),
                authorId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000041")),
            )
        given(findTicketByIdUsecase.find(FindTicketByIdQuery(ticketId))).willReturn(ticket)

        val view =
            controller.endTicketEarly(
                admin,
                ticketId.id,
                mock(RedirectAttributes::class.java),
                htmxRequest(isHtmx = true),
                ModelMap(),
            )

        assertEquals("admin/fragments/ticket-ended-banner :: ticketEndedBanner", view)
        val commandCaptor = org.mockito.ArgumentCaptor.forClass(CancelTicketCommand::class.java)
        verify(cancelTicketUsecase).cancel(commandCaptor.capture() ?: CancelTicketCommand(ticketId, admin))
        assertEquals(ticketId, commandCaptor.value.ticketId)
    }

    private fun htmxRequest(isHtmx: Boolean = false): HtmxRequest {
        val request = mock(HtmxRequest::class.java)
        given(request.isHtmxRequest).willReturn(isHtmx)
        return request
    }

    private fun staffUser(): UserIdentitySnapshot =
        user(UUID.fromString("00000000-0000-0000-0000-000000000010"), setOf(UserRole.WIFI_STAFF))

    private fun adminUser(): UserIdentitySnapshot =
        user(UUID.fromString("00000000-0000-0000-0000-000000000020"), setOf(UserRole.WIFI_ADMIN))

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
