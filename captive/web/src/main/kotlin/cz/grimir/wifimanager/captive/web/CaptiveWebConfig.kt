package cz.grimir.wifimanager.captive.web

import cz.grimir.wifimanager.captive.web.security.CaptiveDeviceSessionFingerprintInterceptor
import cz.grimir.wifimanager.captive.web.portal.CaptivePortalApiProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@ComponentScan
@EnableConfigurationProperties(CaptivePortalApiProperties::class)
class CaptiveWebConfig(
    private val captiveDeviceSessionFingerprintInterceptor: CaptiveDeviceSessionFingerprintInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(captiveDeviceSessionFingerprintInterceptor)
            .addPathPatterns("/captive/device", "/captive/device/**")
    }
}
