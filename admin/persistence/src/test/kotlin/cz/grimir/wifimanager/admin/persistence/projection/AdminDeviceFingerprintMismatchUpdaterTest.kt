package cz.grimir.wifimanager.admin.persistence.projection

import com.fasterxml.jackson.databind.ObjectMapper
import cz.grimir.wifimanager.admin.persistence.AdminDeviceFingerprintMismatchJpaRepository
import cz.grimir.wifimanager.admin.persistence.entity.AdminDeviceFingerprintMismatchEntity
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.events.DeviceFingerprintActionTaken
import cz.grimir.wifimanager.shared.events.DeviceFingerprintMismatchDetectedEvent
import cz.grimir.wifimanager.shared.events.DeviceFingerprintSubjectType
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.UUID

class AdminDeviceFingerprintMismatchUpdaterTest {
    private val repository: AdminDeviceFingerprintMismatchJpaRepository = mock()
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val updater = AdminDeviceFingerprintMismatchUpdater(repository = repository, objectMapper = objectMapper)

    @Test
    fun `append stores full mismatch payload`() {
        val event = mismatchEvent()

        updater.append(event)

        val entityCaptor = argumentCaptor<AdminDeviceFingerprintMismatchEntity>()
        verify(repository).save(entityCaptor.capture())
        val entity = entityCaptor.firstValue
        assertEquals(DeviceFingerprintSubjectType.NETWORK_USER_DEVICE.name, entity.subjectType)
        assertEquals(event.userId?.id, entity.userId)
        assertEquals(event.ticketId?.id, entity.ticketId)
        assertEquals(event.deviceMac, entity.deviceMac)
        assertEquals(event.score, entity.score)
        assertEquals(event.breached, entity.breached)
        assertEquals(event.actionTaken.name, entity.actionTaken)
        assertArrayEquals(event.reasons.toTypedArray(), entity.reasons)
        assertEquals(objectMapper.readTree(event.previousFingerprint), entity.previousFingerprint)
        assertEquals(objectMapper.readTree(event.currentFingerprint), entity.currentFingerprint)
        assertEquals(objectMapper.readTree(event.previousSources), entity.previousSources)
        assertEquals(objectMapper.readTree(event.currentSources), entity.currentSources)
        assertEquals(event.detectedAt, entity.detectedAt)
    }

    @Test
    fun `append saves repeated mismatch events as separate rows`() {
        val first = mismatchEvent()
        val second =
            mismatchEvent().copy(
                score = 90,
                breached = true,
                actionTaken = DeviceFingerprintActionTaken.REAUTH_REQUIRED,
                detectedAt = Instant.parse("2025-01-01T10:16:00Z"),
            )

        updater.append(first)
        updater.append(second)

        val entityCaptor = argumentCaptor<AdminDeviceFingerprintMismatchEntity>()
        verify(repository, org.mockito.kotlin.times(2)).save(entityCaptor.capture())
        assertEquals(2, entityCaptor.allValues.size)
        assertEquals(first.score, entityCaptor.allValues[0].score)
        assertEquals(second.score, entityCaptor.allValues[1].score)
        assertEquals(first.detectedAt, entityCaptor.allValues[0].detectedAt)
        assertEquals(second.detectedAt, entityCaptor.allValues[1].detectedAt)
    }

    private fun mismatchEvent(): DeviceFingerprintMismatchDetectedEvent =
        DeviceFingerprintMismatchDetectedEvent(
            subjectType = DeviceFingerprintSubjectType.NETWORK_USER_DEVICE,
            userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000111")),
            ticketId = TicketId(UUID.fromString("00000000-0000-0000-0000-000000000222")),
            deviceMac = "aa:bb:cc:dd:ee:ff",
            score = 45,
            breached = false,
            actionTaken = DeviceFingerprintActionTaken.LOG_ONLY,
            reasons = listOf("dhcp_vendor_class-mismatch"),
            previousFingerprint = """{"signals":{"dhcp_vendor_class":{"value":"android-dhcp-14"}}}""",
            currentFingerprint = """{"signals":{"dhcp_vendor_class":{"value":"android-dhcp-15"}}}""",
            previousSources = """{"dhcp_vendor_class":"router-agent:dhcp-log"}""",
            currentSources = """{"dhcp_vendor_class":"router-agent:dhcp-log"}""",
            detectedAt = Instant.parse("2025-01-01T10:15:00Z"),
        )
}
