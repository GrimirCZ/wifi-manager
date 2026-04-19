package cz.grimir.wifimanager.shared.security.mvc

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class SharedSecurityWebMvcConfig(
    private val requestMdcInterceptor: RequestMdcInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(requestMdcInterceptor)
    }
}
