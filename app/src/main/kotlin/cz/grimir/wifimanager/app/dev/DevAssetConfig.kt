package cz.grimir.wifimanager.app.dev

import cz.grimir.wifimanager.app.WebMvcConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry

@Profile("dev")
@Configuration
@Order(1)
class DevAssetConfig(
    argumentResolvers: List<HandlerMethodArgumentResolver>,
) : WebMvcConfig(argumentResolvers) {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val root = System.getenv("PROJECT_ROOT") ?: "."

        registry
            .addResourceHandler("/assets/admin/**")
            .addResourceLocations("file:$root/admin/web/src/main/resources/static/assets/admin/")
            .setCachePeriod(0)

        registry
            .addResourceHandler("/assets/captive/**")
            .addResourceLocations("file:$root/captive/web/src/main/resources/static/assets/captive/")
            .setCachePeriod(0)

        registry
            .addResourceHandler("/assets/shared/**")
            .addResourceLocations("file:$root/shared/ui/src/main/resources/static/assets/shared/")
            .setCachePeriod(0)
    }
}
