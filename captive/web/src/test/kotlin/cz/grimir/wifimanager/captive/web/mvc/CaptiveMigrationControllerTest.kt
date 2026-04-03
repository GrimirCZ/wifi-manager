package cz.grimir.wifimanager.captive.web.mvc

import cz.grimir.wifimanager.captive.application.migration.handler.command.ImportMigratedCaptiveUserUsecase
import cz.grimir.wifimanager.captive.application.migration.model.MigrationImportResult
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveMigrationImportRequest
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveMigrationImportedDeviceRequest
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveMigrationImportedTicketRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class CaptiveMigrationControllerTest {
    private val importMigratedCaptiveUserUsecase: ImportMigratedCaptiveUserUsecase = mock()
    private val controller = CaptiveMigrationController(importMigratedCaptiveUserUsecase)

    @Test
    fun `returns import response for valid request`() {
        whenever(importMigratedCaptiveUserUsecase.importUser(org.mockito.kotlin.any())).thenReturn(
            MigrationImportResult(
                userId = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                identityId = UUID.fromString("00000000-0000-0000-0000-000000000222"),
                email = "user@example.com",
                importedTicketCount = 1,
                importedDeviceCount = 2,
            ),
        )

        val response =
            controller.importUser(
                CaptiveMigrationImportRequest(
                    email = "user@example.com",
                    tickets =
                        listOf(
                            CaptiveMigrationImportedTicketRequest(
                                start = Instant.parse("2026-04-03T10:00:00Z"),
                                lengthSeconds = 3600,
                                authorizedDevices =
                                    listOf(
                                        CaptiveMigrationImportedDeviceRequest(
                                            mac = "AA:BB:CC:DD:EE:FF",
                                            deviceName = "iPhone",
                                            displayName = "John phone",
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        assertEquals("user@example.com", response.email)
        assertEquals(1, response.importedTicketCount)
        assertEquals(2, response.importedDeviceCount)
    }
}
