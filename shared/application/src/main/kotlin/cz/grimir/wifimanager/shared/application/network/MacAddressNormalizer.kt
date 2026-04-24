package cz.grimir.wifimanager.shared.application.network

import java.util.Locale

object MacAddressNormalizer {
    fun normalize(raw: String): String =
        raw
            .trim()
            .replace("-", ":")
            .lowercase(Locale.ROOT)
}
