package cz.grimir.wifimanager.admin.web.util

import cz.grimir.wifimanager.shared.ui.AccessCodeFormatter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataMatrixBuilderTest {
    private val builder = DataMatrixBuilder(AccessCodeFormatter())

    @Test
    fun `creates svg data uri for access code`() {
        val dataUri = builder.createAccessCodeDataMatrix("ABC-DEF")

        assertTrue(dataUri.startsWith("data:image/svg+xml;charset=UTF-8,"))
        assertTrue(dataUri.contains("%3Csvg"))
    }
}
