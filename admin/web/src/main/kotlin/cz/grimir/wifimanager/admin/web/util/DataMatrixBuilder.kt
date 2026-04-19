package cz.grimir.wifimanager.admin.web.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.datamatrix.encoder.SymbolShapeHint
import cz.grimir.wifimanager.shared.ui.AccessCodeFormatter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.springframework.stereotype.Component

@Component("DataMatrixBuilder")
class DataMatrixBuilder(
    private val accessCodeFormatter: AccessCodeFormatter,
) {
    fun createAccessCodeDataMatrix(code: String): String {
        val payload = accessCodeFormatter.createBarcodePayload(code)
        val matrix =
            MultiFormatWriter().encode(
                payload,
                BarcodeFormat.DATA_MATRIX,
                1,
                1,
                mapOf(
                    EncodeHintType.DATA_MATRIX_SHAPE to SymbolShapeHint.FORCE_SQUARE,
                    EncodeHintType.MARGIN to 2,
                ),
            )

        return svgDataUri(renderSvg(matrix))
    }

    private fun renderSvg(matrix: BitMatrix): String {
        val size = 180
        val moduleSize = maxOf(4, size / maxOf(matrix.width, matrix.height))
        val svgWidth = matrix.width * moduleSize
        val svgHeight = matrix.height * moduleSize
        val body =
            buildString {
                append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 $svgWidth $svgHeight' shape-rendering='crispEdges'>")
                append("<rect width='100%' height='100%' fill='white'/>")
                for (y in 0 until matrix.height) {
                    for (x in 0 until matrix.width) {
                        if (matrix[x, y]) {
                            append("<rect x='${x * moduleSize}' y='${y * moduleSize}' width='$moduleSize' height='$moduleSize' fill='black'/>")
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
