package cz.grimir.wifimanager.admin.web.util

import cz.grimir.wifimanager.shared.ui.AccessCodeFormatter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class TicketQrCodeBuilderTest {
    private val builder = TicketQrCodeBuilder(AccessCodeFormatter(), "https://portal.example/")

    @Test
    fun `creates svg data uri for access code`() {
        val dataUri = builder.createAccessCodeQrCode("ABC-DEF")

        assertTrue(dataUri.startsWith("data:image/svg+xml;charset=UTF-8,"))
        assertTrue(dataUri.contains("%3Csvg"))
    }

    @Test
    fun `creates svg with explicit intrinsic dimensions`() {
        val dataUri = builder.createAccessCodeQrCode("ABC-DEF")

        val svg = URLDecoder.decode(dataUri.substringAfter(","), StandardCharsets.UTF_8)

        val svgTag = svg.substringBefore(">")
        assertTrue(svgTag.contains(" width='"))
        assertTrue(svgTag.contains(" height='"))
    }

    @Test
    fun `creates captive portal url with normalized code parameter`() {
        val url = builder.createCaptivePortalUrl("abc-def")

        assertEquals("https://portal.example/captive?code=ABCDEF", url)
    }
}
