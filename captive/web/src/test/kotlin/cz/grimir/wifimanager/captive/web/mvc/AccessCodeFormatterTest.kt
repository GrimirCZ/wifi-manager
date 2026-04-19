package cz.grimir.wifimanager.captive.web.mvc

import cz.grimir.wifimanager.shared.ui.AccessCodeFormatter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AccessCodeFormatterTest {
    private val formatter = AccessCodeFormatter()

    @Test
    fun `barcode payload round trips access code`() {
        val payload = formatter.createBarcodePayload("ABC-DEF")

        assertEquals("ABCDEF", formatter.parseBarcodePayload(payload))
    }

    @Test
    fun `barcode payload parser rejects invalid checksum`() {
        val payload = formatter.createBarcodePayload("ABCDEF").dropLast(1) + "Z"

        assertNull(formatter.parseBarcodePayload(payload))
    }
}
