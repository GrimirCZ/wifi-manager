package cz.grimir.wifimanager.admin.web.security.support

/**
 * Annotation to inject the currently authenticated user into a controller method parameter.
 * The user is resolved from the OidcUser authentication principal.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CurrentUser
