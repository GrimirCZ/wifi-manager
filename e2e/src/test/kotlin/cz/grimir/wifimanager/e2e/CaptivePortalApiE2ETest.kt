package cz.grimir.wifimanager.e2e

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class CaptivePortalApiE2ETest : BaseWorkflowE2ETest() {
    @Test
    fun `captive api reflects ticket authorization before and after expiry`() {
        val tokenId = UUID.randomUUID()
        val accessCode = "E2EAPI01"
        val validUntil = Instant.now().plusSeconds(180)

        seedAuthorizedTicketDevice(
            tokenId = tokenId,
            accessCode = accessCode,
            validUntil = validUntil,
            macAddress = DEFAULT_DEVICE_MAC,
            deviceName = "e2e-device",
        )

        val beforeResponse = page.request().get("$baseUrl/captive/api")
        val beforeBody = beforeResponse.text()

        assertEquals(200, beforeResponse.status())
        assertEquals("application/captive+json", beforeResponse.headers()["content-type"])
        assertTrue(beforeBody.contains(""""captive":false"""))
        val beforeRemaining = extractSecondsRemaining(beforeBody)
        assertTrue(beforeRemaining in 1..180)

        jdbcTemplate.update(
            """
            update captive.captive_authorization_token
            set valid_until = ?
            where id = ?
            """.trimIndent(),
            Timestamp.from(Instant.now().minusSeconds(5)),
            tokenId,
        )

        val afterResponse = page.request().get("$baseUrl/captive/api")
        val afterBody = afterResponse.text()

        assertEquals(200, afterResponse.status())
        assertTrue(afterBody.contains(""""captive":true"""))
        assertFalse(afterBody.contains("seconds-remaining"))
    }

    private fun seedAuthorizedTicketDevice(
        tokenId: UUID,
        accessCode: String,
        validUntil: Instant,
        macAddress: String,
        deviceName: String,
    ) {
        jdbcTemplate.update(
            """
            insert into captive.captive_authorization_token (
                id,
                access_code,
                valid_until,
                require_user_name_on_login,
                kicked_macs
            ) values (?, ?, ?, false, cast('{}' as text[]))
            """.trimIndent(),
            tokenId,
            accessCode,
            Timestamp.from(validUntil),
        )

        jdbcTemplate.update(
            """
            insert into captive.captive_device (
                mac,
                display_name,
                device_name,
                fingerprint_profile,
                fingerprint_status,
                fingerprint_verified_at,
                reauth_required_at
            ) values (?, null, ?, null, 'NONE', null, null)
            """.trimIndent(),
            macAddress,
            deviceName,
        )

        jdbcTemplate.update(
            """
            insert into captive.captive_authorized_device (
                token_id,
                device_mac
            ) values (?, ?)
            """.trimIndent(),
            tokenId,
            macAddress,
        )
    }

    private fun extractSecondsRemaining(body: String): Long {
        val match = Regex(""""seconds-remaining":(\d+)""").find(body)
        require(match != null) { "seconds-remaining missing from response: $body" }
        return match.groupValues[1].toLong()
    }
}
