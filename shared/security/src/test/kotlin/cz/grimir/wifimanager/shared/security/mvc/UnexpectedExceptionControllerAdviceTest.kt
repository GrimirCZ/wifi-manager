package cz.grimir.wifimanager.shared.security.mvc

import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.ModelAndView

class UnexpectedExceptionControllerAdviceTest {
    private val advice = UnexpectedExceptionControllerAdvice("School Wi-Fi")

    @Test
    fun `returns admin full-page error for unexpected admin exception`() {
        val request = request("GET", "/admin/devices")

        val response = advice.handle(IllegalStateException("boom"), request)

        val modelAndView = response as ModelAndView
        assertThat(modelAndView.viewName).isEqualTo("admin/error")
        assertThat(modelAndView.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(modelAndView.model["returnHref"]).isEqualTo("/admin")
    }

    @Test
    fun `returns captive htmx fragment for unexpected captive exception`() {
        val request =
            request("POST", "/captive").apply {
                addHeader("HX-Request", "true")
            }

        val response = advice.handle(IllegalStateException("boom"), request)

        val modelAndView = response as ModelAndView
        assertThat(modelAndView.viewName).isEqualTo("captive/fragments/error :: errorContent")
        assertThat(modelAndView.status).isEqualTo(HttpStatus.OK)
        assertThat(modelAndView.model["returnHref"]).isEqualTo("/captive")
    }

    @Test
    fun `returns captive error copy for missing client mac`() {
        val request = request("POST", "/captive/login")

        val response = advice.handle(MissingClientMacException(), request)

        val modelAndView = response as ModelAndView
        assertThat(modelAndView.viewName).isEqualTo("captive/error")
        assertThat(modelAndView.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(modelAndView.model["missingClientMac"]).isEqualTo(true)
        assertThat(modelAndView.model["errorNetworkName"]).isEqualTo("School Wi-Fi")
    }

    @Test
    fun `returns json error for captive api exception`() {
        val request = request("GET", "/captive/api")

        val response = advice.handle(IllegalStateException("boom"), request)

        val entity = response as ResponseEntity<*>
        assertThat(entity.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(entity.body).isEqualTo(
            mapOf(
                "error" to "internal_server_error",
                "message" to "Unexpected server error. Please try again later.",
            ),
        )
    }

    @Test
    fun `rethrows expected spring http exceptions`() {
        val request = request("GET", "/admin")

        assertThrows<ResponseStatusException> {
            advice.handle(ResponseStatusException(HttpStatus.NOT_FOUND), request)
        }
    }

    private fun request(
        method: String,
        uri: String,
    ): MockHttpServletRequest = MockHttpServletRequest(method, uri)
}
