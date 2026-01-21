package cz.grimir.wifimanager.shared.application

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class KeyValueMapParser(
    private val entrySeparator: String = ";",
    private val keyValueSeparator: String = "=",
) {
    fun parse(raw: String): Map<String, String> = parse(raw) { it }

    fun <T> parse(
        raw: String,
        valueParser: (String) -> T?,
    ): Map<String, T> {
        if (raw.isBlank()) {
            return emptyMap()
        }

        val result = mutableMapOf<String, T>()
        raw.split(entrySeparator).forEach { entry ->
            val trimmed = entry.trim()
            if (trimmed.isBlank()) {
                return@forEach
            }
            val parts = trimmed.split(keyValueSeparator, limit = 2)
            if (parts.size != 2) {
                logger.warn { "Invalid entry '$trimmed' (expected key${keyValueSeparator}value)." }
                return@forEach
            }
            val key = parts[0].trim()
            val valueRaw = parts[1].trim()
            if (key.isBlank() || valueRaw.isBlank()) {
                logger.warn { "Invalid entry '$trimmed' (expected key${keyValueSeparator}value)." }
                return@forEach
            }
            val value = valueParser(valueRaw)
            if (value == null) {
                logger.warn { "Invalid entry '$trimmed' (unable to parse value)." }
                return@forEach
            }
            result[key] = value
        }
        return result
    }
}
