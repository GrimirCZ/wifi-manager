package cz.grimir.wifimanager.shared.application.identity.google

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.google.directory")
data class GoogleDirectoryApiProperties(
    val directoryCustomer: String = "my_customer",
    val serviceAccountJsonPath: String = "",
    val impersonatedUser: String? = null,
    val directoryApiBaseUrl: String = "https://admin.googleapis.com/admin/directory/v1",
) {
    fun isConfigured(): Boolean = serviceAccountJsonPath.isNotBlank()
}
