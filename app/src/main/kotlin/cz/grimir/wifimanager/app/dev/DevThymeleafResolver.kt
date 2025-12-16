package cz.grimir.wifimanager.app.dev

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.templatemode.TemplateMode

@Profile("dev")
@Configuration
class DevThymeleafFileResolvers {

    @Bean
    fun adminFileResolver(): SpringResourceTemplateResolver =
        resolver(
            name = "adminFile",
            prefixRelToRoot = "admin/web/src/main/resources/templates/",
            patterns = setOf("admin/**"),
            order = 0
        )

    @Bean
    fun captiveFileResolver(): SpringResourceTemplateResolver =
        resolver(
            name = "captiveFile",
            prefixRelToRoot = "captive/web/src/main/resources/templates/",
            patterns = setOf("captive/**"),
            order = 0
        )

    private fun resolver(
        name: String,
        prefixRelToRoot: String,
        patterns: Set<String>,
        order: Int
    ): SpringResourceTemplateResolver {
        val root = System.getenv("PROJECT_ROOT") ?: "."

        return SpringResourceTemplateResolver().apply {
            this.name = name
            this.order = order
            this.prefix = "file:$root/$prefixRelToRoot"
            this.suffix = ".html"
            this.templateMode = TemplateMode.HTML
            this.characterEncoding = "UTF-8"
            this.isCacheable = false
            this.checkExistence = true
            this.resolvablePatterns = patterns
        }
    }
}
