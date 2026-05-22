package cz.grimir.wifimanager.admin.web.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import cz.grimir.wifimanager.shared.ui.AccessCodeFormatter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component("TicketQrCodeBuilder")
class TicketQrCodeBuilder(
    private val accessCodeFormatter: AccessCodeFormatter,
    @param:Value("\${wifimanager.captive.portal.public-base-url}")
    private val captivePortalPublicBaseUrl: String,
) {
    fun createAccessCodeQrCode(code: String): String {
        val payload = createCaptivePortalUrl(code)
        val matrix =
            MultiFormatWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                1,
                1,
                mapOf(
                    EncodeHintType.MARGIN to 2,
                ),
            )

        return svgDataUri(renderSvg(matrix))
    }

    fun createCaptivePortalUrl(code: String): String {
        val normalizedCode = accessCodeFormatter.normalize(code)
        require(accessCodeFormatter.isValidNormalized(normalizedCode)) { "Access code must be 6 alphanumeric characters." }
        return UriComponentsBuilder
            .fromUriString(captivePortalPublicBaseUrl.trimEnd('/'))
            .path("/captive")
            .queryParam("code", normalizedCode)
            .build()
            .toUriString()
    }

    private fun renderSvg(matrix: BitMatrix): String {
        val targetSymbolSize = 180
        val moduleSize = maxOf(4, targetSymbolSize / maxOf(matrix.width, matrix.height))
        val symbolWidth = matrix.width * moduleSize
        val symbolHeight = matrix.height * moduleSize
        val body =
            buildString {
                append(
                    "<svg xmlns='http://www.w3.org/2000/svg' width='$symbolWidth' height='$symbolHeight' viewBox='0 0 $symbolWidth $symbolHeight' shape-rendering='crispEdges'>",
                )
                append("<rect width='100%' height='100%' fill='white'/>")
                for (y in 0 until matrix.height) {
                    for (x in 0 until matrix.width) {
                        if (matrix[x, y]) {
                            append(
                                "<rect x='${x * moduleSize}' y='${y * moduleSize}' width='$moduleSize' height='$moduleSize' fill='black'/>",
                            )
                        }
                    }
                }
                append("</svg>")
            }
        return body
    }

    private fun svgDataUri(svg: String): String =
        "data:image/svg+xml;charset=UTF-8," + URLEncoder.encode(svg, StandardCharsets.UTF_8).replace("+", "%20")
}
