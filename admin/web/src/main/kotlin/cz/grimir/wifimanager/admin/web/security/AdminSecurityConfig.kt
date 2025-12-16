package cz.grimir.wifimanager.admin.web.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
class AdminSecurityConfig(
    private val properties: AdminSecurityProperties,
) {
    @Bean
    @Order(10)
    fun adminSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/admin/**")
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/admin/login").permitAll()
                    .anyRequest().hasRole("ADMIN")
            }
            .formLogin { login ->
                login
                    .loginPage("/admin/login")
                    .defaultSuccessUrl("/admin", true)
                    .permitAll()
            }
            .logout { logout ->
                logout
                    .logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/admin/login?logout")
            }
            .httpBasic(withDefaults())

        return http.build()
    }

    @Bean
    fun adminUserDetailsService(): UserDetailsService {
        val admin =
            User
                .withUsername(properties.username)
                .password("{noop}${properties.password}")
                .roles("ADMIN")
                .build()
        return InMemoryUserDetailsManager(admin)
    }
}

