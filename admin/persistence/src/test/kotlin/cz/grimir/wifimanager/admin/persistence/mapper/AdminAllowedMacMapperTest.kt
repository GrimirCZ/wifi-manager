package cz.grimir.wifimanager.admin.persistence.mapper

import cz.grimir.wifimanager.admin.core.value.AllowedMac
import cz.grimir.wifimanager.admin.persistence.entity.AdminAllowedMacEntity
import cz.grimir.wifimanager.shared.core.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AdminAllowedMacMapperTest {
    private val mapper = AdminAllowedMacMapper()

    @Test
    fun `maps note from entity to domain`() {
        val entity =
            AdminAllowedMacEntity(
                mac = "aa:bb:cc:dd:ee:ff",
                ownerUserId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                ownerDisplayName = "Owner",
                ownerEmail = "owner@example.com",
                note = "printer in lab",
                hostname = "lab-printer",
                validUntil = Instant.parse("2025-01-01T10:00:00Z"),
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                updatedAt = Instant.parse("2025-01-01T09:30:00Z"),
            )

        val domain = mapper.toDomain(entity)

        assertEquals("printer in lab", domain.note)
        assertEquals(UserId(entity.ownerUserId), domain.ownerUserId)
    }

    @Test
    fun `maps note from domain to entity`() {
        val domain =
            AllowedMac(
                mac = "aa:bb:cc:dd:ee:ff",
                hostname = "lab-printer",
                ownerUserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                ownerDisplayName = "Owner",
                ownerEmail = "owner@example.com",
                note = "printer in lab",
                validUntil = Instant.parse("2025-01-01T10:00:00Z"),
                createdAt = Instant.parse("2025-01-01T09:00:00Z"),
                updatedAt = Instant.parse("2025-01-01T09:30:00Z"),
            )

        val entity = mapper.toEntity(domain)

        assertEquals("printer in lab", entity.note)
        assertEquals(domain.ownerUserId.id, entity.ownerUserId)
    }
}
