package cz.grimir.wifimanager.admin.web.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class AdminSecurityConfig(
    private val oidcUserService: AdminOidcUserService,
) {
    @Bean
    @Order(10)
    fun adminSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/admin/**", "/oauth2/**", "/login/oauth2/**")
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/admin/login")
                    .permitAll()
                    .requestMatchers("/assets/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.oauth2Login { login ->
                login
                    .loginPage("/admin/login")
                    .defaultSuccessUrl("/admin", true)
                    .userInfoEndpoint { userInfo -> userInfo.oidcUserService(oidcUserService) }
            }.logout { logout ->
                logout
                    .logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/admin/login?logout")
            }

        return http.build()
    }
}
