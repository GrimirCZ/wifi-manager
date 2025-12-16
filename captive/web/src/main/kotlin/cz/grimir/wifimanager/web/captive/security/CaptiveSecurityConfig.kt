package cz.grimir.wifimanager.web.captive.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class CaptiveSecurityConfig {
    @Bean
    @Order(20)
    fun captiveSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/captive/**")
            .authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }
            .csrf { csrf -> csrf.disable() }
        return http.build()
    }
}
