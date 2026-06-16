package cz.grimir.wifimanager.shared.security.mvc

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.ModelAndView
import java.io.IOException

private val logger = KotlinLogging.logger {}

private const val OUTPUT_STREAM_ALREADY_USED_MESSAGE = "getOutputStream() has already been called for this response"

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class UnexpectedExceptionControllerAdvice(
    @param:Value($$"${wifimanager.wifi.ssid:WiFi}")
    private val wifiSsid: String,
) {
    @ExceptionHandler(Exception::class)
    fun handle(
        exception: Exception,
        request: HttpServletRequest,
    ): Any {
        if (exception is ErrorResponse || exception is AccessDeniedException) {
            throw exception
        }

        val requestPath = request.requestURI.orEmpty()
        val isHtmx = request.getHeader("HX-Request").equals("true", ignoreCase = true)

        val clientDisconnectReason = exception.getClientDisconnectReason()
        if (clientDisconnectReason != null) {
            logger.warn {
                "Client disconnected during web response method=${request.method} path=$requestPath htmx=$isHtmx reason=$clientDisconnectReason"
            }
            return ModelAndView()
        }

        logger.error(exception) {
            "Unhandled web exception method=${request.method} path=$requestPath htmx=$isHtmx"
        }

        if (requestPath.startsWith("/captive/api")) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    mapOf(
                        "error" to "internal_server_error",
                        "message" to "Unexpected server error. Please try again later.",
                    ),
                )
        }

        val isCaptiveRequest = requestPath.startsWith("/captive")
        val errorModel =
            if (isCaptiveRequest && exception is MissingClientMacException) {
                mapOf(
                    "missingClientMac" to true,
                    "errorNetworkName" to wifiSsid,
                )
            } else {
                emptyMap()
            }
        val viewName =
            when {
                isCaptiveRequest && isHtmx -> "captive/fragments/error :: errorContent"
                isCaptiveRequest -> "captive/error"
                isHtmx -> "admin/fragments/error :: errorContent"
                else -> "admin/error"
            }
        val returnHref = if (isCaptiveRequest) "/captive" else "/admin"
        val status = if (isHtmx) HttpStatus.OK else HttpStatus.INTERNAL_SERVER_ERROR

        return ModelAndView(
            viewName,
            mapOf("returnHref" to returnHref) + errorModel,
            status,
        )
    }
}

private fun Throwable.getClientDisconnectReason(): String? {
    val seen = mutableSetOf<Throwable>()
    var current: Throwable? = this

    while (current != null && seen.add(current)) {
        val message = current.message.orEmpty()

        when {
            current.javaClass.name == "org.apache.catalina.connector.ClientAbortException" -> {
                return "client-abort"
            }

            current is IOException && message.contains("Broken pipe", ignoreCase = true) -> {
                return "broken-pipe"
            }

            current is IOException && message.contains("Connection reset", ignoreCase = true) -> {
                return "connection-reset"
            }

            current is IllegalStateException && message.contains(OUTPUT_STREAM_ALREADY_USED_MESSAGE, ignoreCase = true) -> {
                return "output-stream-already-used"
            }
        }

        current = current.cause
    }

    return null
}
