package cz.grimir.wifimanager.admin.application.ticket.support

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class AccessCodeGenerator(
    val random: SecureRandom = SecureRandom(),
) {
    private val chars = "BCDFGHJKMPQRTVWXY2346789"

    fun generate(length: Int = 6): String =
        (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
}
