package cz.grimir.wifimanager.app

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter
import org.springframework.web.servlet.resource.VersionResourceResolver
import java.util.concurrent.TimeUnit

@Profile("!dev")
@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .resourceChain(true)
            .addResolver(
                // add file hash to filename
                VersionResourceResolver()
                    .addContentVersionStrategy("/**"),
            )
    }

    @Bean
    fun resourceUrlEncodingFilter(): ResourceUrlEncodingFilter = ResourceUrlEncodingFilter()
}
