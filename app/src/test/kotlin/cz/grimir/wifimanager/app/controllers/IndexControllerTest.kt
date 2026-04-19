package cz.grimir.wifimanager.app.controllers

import cz.grimir.wifimanager.app.routing.CaptiveRequestRoutingProperties
import cz.grimir.wifimanager.app.routing.CaptiveSubnetMatcher
import cz.grimir.wifimanager.shared.security.UiSecurityProperties
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class IndexControllerTest {
    private val securityProperties =
        UiSecurityProperties(
            oidcRegistrationId = "admin",
            authoritiesProviderByRegistration = mapOf("admin" to "keycloak-realm-roles"),
        )

    @Test
    fun `anonymous request from non-captive ip renders landing page`() {
        mvc(CaptiveSubnetMatcher(CaptiveRequestRoutingProperties()))
            .perform(get("/").withRemoteAddr("203.0.113.15"))
            .andExpect(status().isOk)
            .andExpect(view().name("admin/landing"))
            .andExpect(model().attribute("oidcRegistrationId", "admin"))
    }

    @Test
    fun `authenticated request from non-captive ip redirects to admin`() {
        mvc(CaptiveSubnetMatcher(CaptiveRequestRoutingProperties()))
            .perform(
                get("/")
                    .withRemoteAddr("203.0.113.15")
                    .principal(TestingAuthenticationToken("user", "password", "ROLE_USER").apply { isAuthenticated = true }),
            )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin"))
    }

    @Test
    fun `anonymous request from configured ipv4 subnet redirects to captive`() {
        mvc(CaptiveSubnetMatcher(CaptiveRequestRoutingProperties(ipv4Subnets = listOf("10.42.0.0/16"))))
            .perform(get("/").withRemoteAddr("10.42.8.19"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/captive"))
    }

    @Test
    fun `anonymous request from configured ipv6 subnet redirects to captive`() {
        mvc(CaptiveSubnetMatcher(CaptiveRequestRoutingProperties(ipv6Subnets = listOf("2001:db8::/64"))))
            .perform(get("/").withRemoteAddr("2001:db8::abcd"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/captive"))
    }

    @Test
    fun `authenticated request from captive subnet still redirects to captive`() {
        mvc(CaptiveSubnetMatcher(CaptiveRequestRoutingProperties(ipv4Subnets = listOf("10.42.0.0/16"))))
            .perform(
                get("/")
                    .withRemoteAddr("10.42.8.19")
                    .principal(TestingAuthenticationToken("user", "password", "ROLE_USER").apply { isAuthenticated = true }),
            )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/captive"))
    }

    private fun mvc(captiveSubnetMatcher: CaptiveSubnetMatcher): MockMvc =
        MockMvcBuilders
            .standaloneSetup(IndexController(securityProperties, captiveSubnetMatcher))
            .build()

    private fun org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder.withRemoteAddr(remoteAddr: String) =
        with { request ->
            request.remoteAddr = remoteAddr
            request
        }
}
