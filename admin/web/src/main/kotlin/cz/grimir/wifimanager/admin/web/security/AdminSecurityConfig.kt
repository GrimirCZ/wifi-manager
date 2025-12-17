package cz.grimir.wifimanager.admin.web.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.SecurityFilterChain

/**
 * Admin-area Spring Security configuration.
 *
 * - Uses OIDC login (provider configured by the shell app).
 * - Propagates logout to the OIDC provider (end-session endpoint) so SSO sessions are cleared.
 */
@Configuration
class AdminSecurityConfig(
    private val oidcUserService: AdminOidcUserService,
) {
    @Bean
    @Order(10)
    fun adminSecurityFilterChain(
        http: HttpSecurity,
        clientRegistrationRepository: ClientRegistrationRepository,
    ): SecurityFilterChain {
        val logoutSuccessHandler =
            OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository).also {
                it.setPostLogoutRedirectUri("{baseUrl}/admin/login?logout")
            }

        http
            .securityMatcher("/admin/**", "/oauth2/**", "/login/oauth2/**")
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/admin/login")
                    .permitAll()
                    .requestMatchers("/oauth2/**", "/login/oauth2/**")
                    .permitAll()
                    .requestMatchers("/assets/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.oauth2Login { login ->
                login
                    .loginPage("/admin/login")
                    .defaultSuccessUrl("/admin", true)
                    .authorizationEndpoint { endpoint ->
                        endpoint.authorizationRequestResolver(
                            AdminAuthorizationRequestResolver(
                                clientRegistrationRepository,
                            ),
                        )
                    }.userInfoEndpoint { userInfo -> userInfo.oidcUserService(oidcUserService) }
            }.logout { logout ->
                logout
                    .logoutUrl("/admin/logout")
                    .logoutSuccessHandler(logoutSuccessHandler)
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
            }

        return http.build()
    }
}
