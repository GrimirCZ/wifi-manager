package cz.grimir.wifimanager.shared.ui

import org.springframework.stereotype.Component

@Component("AccessCodeFormatter")
class AccessCodeFormatter {
    companion object {
        private const val BARCODE_PREFIX = "W"
        private val NORMALIZED_CODE_REGEX = Regex("^[A-Z0-9]{8}$")
        private val FORMATTED_CODE_REGEX = Regex("^[A-Z0-9]{3}-[A-Z0-9]{3}-[A-Z0-9]{2}$")
    }

    val barcodePrefix: String
        get() = BARCODE_PREFIX

    fun format(code: String): String = code.chunked(3).joinToString("-")

    fun normalize(code: String): String = code.trim().replace("-", "").uppercase()

    fun formatForInput(code: String): String =
        code
            .trim()
            .uppercase()
            .filter(Char::isLetterOrDigit)
            .take(8)
            .chunked(3)
            .joinToString("-")

    fun isValidNormalized(code: String): Boolean = NORMALIZED_CODE_REGEX.matches(code)

    fun isValidFormatted(code: String): Boolean = FORMATTED_CODE_REGEX.matches(code.trim().uppercase())

    fun isValid(code: String): Boolean {
        val trimmedCode = code.trim()
        return isValidNormalized(normalize(trimmedCode)) &&
            (trimmedCode.contains('-').not() || isValidFormatted(trimmedCode))
    }

    fun createBarcodePayload(code: String): String {
        val normalizedCode = normalize(code)
        require(isValidNormalized(normalizedCode)) { "Access code must be 8 alphanumeric characters." }
        return buildString {
            append(BARCODE_PREFIX)
            append(normalizedCode)
            append(calculateChecksum(normalizedCode))
        }
    }

    fun parseBarcodePayload(payload: String): String? {
        val normalizedPayload =
            payload
                .trim()
                .uppercase()
                .filter(Char::isLetterOrDigit)

        if (!normalizedPayload.startsWith(BARCODE_PREFIX)) {
            return null
        }

        val encodedCode = normalizedPayload.removePrefix(BARCODE_PREFIX)
        if (encodedCode.length != 9) {
            return null
        }

        val accessCode = encodedCode.dropLast(1)
        val checksum = encodedCode.last()
        if (!isValidNormalized(accessCode) || checksum != calculateChecksum(accessCode)) {
            return null
        }

        return accessCode
    }

    private fun calculateChecksum(code: String): Char {
        val checksumValue =
            code.withIndex()
                .sumOf { (index, char) -> (index + 1) * charValue(char) } % 36
        return checksumChar(checksumValue)
    }

    private fun charValue(char: Char): Int =
        when (char) {
            in '0'..'9' -> char - '0'
            in 'A'..'Z' -> 10 + (char - 'A')
            else -> error("Unsupported access code character: $char")
        }

    private fun checksumChar(value: Int): Char =
        if (value < 10) {
            ('0'.code + value).toChar()
        } else {
            ('A'.code + (value - 10)).toChar()
        }
}
