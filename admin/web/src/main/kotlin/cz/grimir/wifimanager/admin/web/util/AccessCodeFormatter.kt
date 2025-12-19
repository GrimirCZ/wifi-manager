package cz.grimir.wifimanager.admin.web.util

import org.springframework.stereotype.Component

@Component("AccessCodeFormatter")
class AccessCodeFormatter {
    fun format(code: String): String {
        return code.chunked(3).joinToString("-")
    }
}