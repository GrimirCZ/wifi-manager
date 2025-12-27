package cz.grimir.wifimanager.shared.ui

import org.springframework.stereotype.Component

@Component("AccessCodeFormatter")
class AccessCodeFormatter {
    fun format(code: String): String = code.chunked(3).joinToString("-")

    fun normalize(code: String): String = code.trim().replace("-", "").uppercase()
}
