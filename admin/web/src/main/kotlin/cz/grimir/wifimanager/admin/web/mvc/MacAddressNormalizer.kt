package cz.grimir.wifimanager.admin.web.mvc

internal object MacAddressNormalizer {
    fun normalize(raw: String): String =
        raw
            .trim()
            .lowercase()
            .replace("-", ":")
}
