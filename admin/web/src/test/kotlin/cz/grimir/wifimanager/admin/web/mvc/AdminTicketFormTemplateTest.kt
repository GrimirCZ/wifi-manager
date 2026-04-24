package cz.grimir.wifimanager.admin.web.mvc

import cz.grimir.wifimanager.admin.web.mvc.dto.CreateTicketRequestDto
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.thymeleaf.context.WebContext
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.web.servlet.JakartaServletWebApplication
import java.util.Locale

class AdminTicketFormTemplateTest {
    private val templateEngine =
        SpringTemplateEngine().apply {
            setTemplateResolver(
                ClassLoaderTemplateResolver().apply {
                    prefix = "templates/"
                    suffix = ".html"
                    templateMode = TemplateMode.HTML
                    characterEncoding = "UTF-8"
                },
            )
        }

    @Test
    fun `staff ticket form renders only standard duration options`() {
        val html = renderForm(canCreateExtendedTickets = false)

        assertTrue(html.contains("""value="120""""))
        assertFalse(html.contains("""value="240""""))
        assertFalse(html.contains("""value="10080""""))
    }

    @Test
    fun `admin ticket form renders standard and extended duration options`() {
        val html = renderForm(canCreateExtendedTickets = true)

        assertTrue(html.contains("""value="120""""))
        assertTrue(html.contains("""value="240""""))
        assertTrue(html.contains("""value="10080""""))
    }

    private fun renderForm(canCreateExtendedTickets: Boolean): String {
        val servletContext = MockServletContext()
        val request = MockHttpServletRequest(servletContext)
        val response = MockHttpServletResponse()
        val exchange = JakartaServletWebApplication.buildApplication(servletContext).buildExchange(request, response)
        val variables =
            mapOf(
                "createTicket" to CreateTicketRequestDto(),
                "canCreateExtendedTickets" to canCreateExtendedTickets,
                "_csrf" to CsrfTokenStub(),
            )
        val context = WebContext(exchange, Locale.ENGLISH, variables)

        return templateEngine.process("admin/fragments/ticket-form", context)
    }

    private class CsrfTokenStub {
        val parameterName: String = "_csrf"
        val token: String = "csrf-token"
    }
}
