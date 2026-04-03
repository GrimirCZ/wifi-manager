package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.migration.command.ImportAdminMigratedTicketsCommand
import cz.grimir.wifimanager.admin.application.migration.handler.command.ImportAdminMigratedTicketsUsecase
import cz.grimir.wifimanager.admin.application.shared.port.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ticket.port.SaveAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.ticket.port.SaveTicketPort
import cz.grimir.wifimanager.admin.application.ticket.support.AccessCodeGenerator
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice
import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.events.TicketCreatedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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
class ImportAdminMigratedTicketsUsecaseTest {
    private val saveTicketPort: SaveTicketPort = mock()
    private val saveAuthorizedDevicePort: SaveAuthorizedDevicePort = mock()
    private val eventPublisher: AdminEventPublisher = mock()
    private val accessCodeGenerator: AccessCodeGenerator = mock()

    private val usecase =
        ImportAdminMigratedTicketsUsecase(
            saveTicketPort = saveTicketPort,
            saveAuthorizedDevicePort = saveAuthorizedDevicePort,
            eventPublisher = eventPublisher,
            accessCodeGenerator = accessCodeGenerator,
        )

    @Test
    fun `creates one admin ticket and devices for one imported ticket`() {
        given(accessCodeGenerator.generate(8)).willReturn("ABCDEFGH")

        val result =
            usecase.import(
                ImportAdminMigratedTicketsCommand(
                    user = testUser(),
                    tickets =
                        listOf(
                            ImportAdminMigratedTicketsCommand.ImportedAdminTicket(
                                start = Instant.parse("2026-04-03T08:00:00Z"),
                                lengthSeconds = 3600,
                                authorizedDevices =
                                    listOf(
                                        ImportAdminMigratedTicketsCommand.ImportedAdminAuthorizedDevice(
                                            mac = "AA:BB:CC:DD:EE:FF",
                                            displayName = "John phone",
                                            deviceName = "iPhone",
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        assertEquals(1, result.size)

        val ticketCaptor = argumentCaptor<Ticket>()
        verify(saveTicketPort).save(ticketCaptor.capture())
        assertEquals(Instant.parse("2026-04-03T08:00:00Z"), ticketCaptor.firstValue.createdAt)
        assertEquals(Instant.parse("2026-04-03T09:00:00Z"), ticketCaptor.firstValue.validUntil)

        val deviceCaptor = argumentCaptor<AuthorizedDevice>()
        verify(saveAuthorizedDevicePort).save(deviceCaptor.capture())
        assertEquals("AA:BB:CC:DD:EE:FF", deviceCaptor.firstValue.mac)
        assertEquals("John phone", deviceCaptor.firstValue.displayName)

        val eventCaptor = argumentCaptor<TicketCreatedEvent>()
        verify(eventPublisher).publish(eventCaptor.capture())
        assertEquals(ticketCaptor.firstValue.id, eventCaptor.firstValue.id)
        assertEquals("ABCDEFGH", eventCaptor.firstValue.accessCode)
    }

    @Test
    fun `creates distinct tickets for multiple imported tickets`() {
        given(accessCodeGenerator.generate(8)).willReturn("AAAAAA11", "BBBBBB22")

        val result =
            usecase.import(
                ImportAdminMigratedTicketsCommand(
                    user = testUser(),
                    tickets =
                        listOf(
                            ImportAdminMigratedTicketsCommand.ImportedAdminTicket(
                                start = Instant.parse("2026-04-03T08:00:00Z"),
                                lengthSeconds = 3600,
                                authorizedDevices = emptyList(),
                            ),
                            ImportAdminMigratedTicketsCommand.ImportedAdminTicket(
                                start = Instant.parse("2026-04-03T10:00:00Z"),
                                lengthSeconds = 7200,
                                authorizedDevices = emptyList(),
                            ),
                        ),
                ),
            )

        assertEquals(2, result.size)
        assertNotEquals(result[0].ticketId, result[1].ticketId)
        assertNotEquals(result[0].accessCode, result[1].accessCode)
    }

    @Test
    fun `duplicate posted ticket still creates another new ticket`() {
        given(accessCodeGenerator.generate(8)).willReturn("AAAAAA11", "BBBBBB22")

        val result =
            usecase.import(
                ImportAdminMigratedTicketsCommand(
                    user = testUser(),
                    tickets =
                        listOf(
                            ImportAdminMigratedTicketsCommand.ImportedAdminTicket(
                                start = Instant.parse("2026-04-03T08:00:00Z"),
                                lengthSeconds = 3600,
                                authorizedDevices = emptyList(),
                            ),
                            ImportAdminMigratedTicketsCommand.ImportedAdminTicket(
                                start = Instant.parse("2026-04-03T08:00:00Z"),
                                lengthSeconds = 3600,
                                authorizedDevices = emptyList(),
                            ),
                        ),
                ),
            )

        assertEquals(2, result.size)
        assertNotEquals(result[0].ticketId, result[1].ticketId)
    }

    private fun testUser(): UserIdentitySnapshot =
        UserIdentitySnapshot(
            userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000111")),
            identityId = UUID.fromString("00000000-0000-0000-0000-000000000222"),
            displayName = "User Example",
            email = "user@example.com",
            pictureUrl = null,
            roles = setOf(UserRole.WIFI_STAFF),
        )
}
