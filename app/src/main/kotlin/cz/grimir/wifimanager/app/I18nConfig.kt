package cz.grimir.wifimanager.app

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import java.time.Duration
import java.util.Locale

@Configuration
class I18nConfig(
    @param:Value("\${app.locale.default:en}")
    private val defaultLocaleTag: String,
) : WebMvcConfigurer {
    @Bean
    fun localeResolver(): LocaleResolver {
        val resolver = CookieLocaleResolver("wm-locale")
        resolver.setDefaultLocale(Locale.forLanguageTag(defaultLocaleTag))
        resolver.setCookiePath("/")
        resolver.setCookieMaxAge(Duration.ofDays(365))
        return resolver
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        val localeChangeInterceptor = LocaleChangeInterceptor()
        localeChangeInterceptor.paramName = "lang"
        registry.addInterceptor(localeChangeInterceptor)
    }
}
