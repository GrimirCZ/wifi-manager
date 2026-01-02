package cz.grimir.wifimanager.shared.security

import cz.grimir.wifimanager.shared.security.oidc.PromptLoginAuthorizationRequestResolver
import cz.grimir.wifimanager.shared.security.oidc.UiOidcUserService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern
import org.springframework.security.web.util.matcher.OrRequestMatcher

@Configuration
@ComponentScan(basePackages = ["cz.grimir.wifimanager.shared.security"])
@EnableConfigurationProperties(UiSecurityProperties::class)
class UiSecurityConfig(
    private val oidcUserService: UiOidcUserService,
    private val resolveUserSuccessHandler: ResolveUserSuccessHandler,
    private val moduleAuthorizationRules: List<ModuleAuthorizationRules>,
) {
    @Bean
    fun uiSecurityFilterChain(
        http: HttpSecurity,
        clientRegistrationRepository: ClientRegistrationRepository,
    ): SecurityFilterChain {
        val logoutSuccessHandler =
            OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository).also {
                it.setPostLogoutRedirectUri("{baseUrl}/")
            }

        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/assets/**")
                    .permitAll()
                    .requestMatchers("/oauth2/**", "/login/oauth2/**")
                    .permitAll()
                    .requestMatchers("/admin/login", "/captive/login")
                    .permitAll()

                moduleAuthorizationRules.forEach { it.configure(auth) }

                auth.anyRequest().authenticated()
            }.oauth2Login { login ->
                login
                    .authorizationEndpoint { endpoint ->
                        endpoint.authorizationRequestResolver(
                            PromptLoginAuthorizationRequestResolver(
                                clientRegistrationRepository,
                            ),
                        )
                    }.userInfoEndpoint { userInfo -> userInfo.oidcUserService(oidcUserService) }
                    .successHandler(resolveUserSuccessHandler)
            }.logout { logout ->
                logout
                    .logoutRequestMatcher(
                        OrRequestMatcher(
                            pathPattern("/admin/logout"),
                            pathPattern("/captive/logout"),
                            pathPattern("/logout"),
                        ),
                    ).logoutSuccessHandler(logoutSuccessHandler)
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
            }.exceptionHandling { exceptions ->
                exceptions
                    .defaultAuthenticationEntryPointFor(
                        LoginUrlAuthenticationEntryPoint("/admin/login"),
                        pathPattern("/admin/**"),
                    ).defaultAuthenticationEntryPointFor(
                        LoginUrlAuthenticationEntryPoint("/captive/login"),
                        pathPattern("/captive/**"),
                    )
            }

        return http.build()
    }
}
