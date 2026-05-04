package cz.grimir.wifimanager.admin.web.mvc

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText

class AdminUserDevicesTemplateTest {
    @Test
    fun `device metadata is rendered as labeled details instead of one inline line`() {
        val template = Path("src/main/resources/templates/admin/fragments/user-devices.html").readText()

        assertTrue(template.contains("admin-device-details"))
        assertTrue(template.contains("admin-device-detail-label"))
        assertTrue(template.contains("admin-device-detail-value"))
        assertFalse(template.contains("sm:gap-x-1.5"))
    }

    @Test
    fun `hostname is exposed in a css popup instead of as a visible detail`() {
        val template = Path("src/main/resources/templates/admin/fragments/user-devices.html").readText()

        assertTrue(template.contains("admin-device-name-popup"))
        assertFalse(template.contains("admin.devices.hostname.field"))
        assertFalse(template.contains("th:title"))
    }

    @Test
    fun `owner renders as a normal grid item`() {
        val template = Path("src/main/resources/templates/admin/fragments/user-devices.html").readText()

        assertFalse(template.contains("admin-device-detail-wide"))
    }

    @Test
    fun `device list does not clip css popups`() {
        val template = Path("src/main/resources/templates/admin/fragments/user-devices.html").readText()

        assertTrue(template.contains("""class="overflow-hidden rounded-2xl border border-border/50 divide-y divide-border/40""""))
        assertTrue(template.contains("admin-device-name-popup"))
    }
}
