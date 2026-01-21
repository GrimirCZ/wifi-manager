package cz.grimir.wifimanager.captive.auth.google

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequestFactory
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.FileInputStream

class GoogleDirectoryApiClient(
    private val properties: GoogleLdapProperties,
) {
    private val credentials: GoogleCredentials = loadCredentials(properties)
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val requestFactory: HttpRequestFactory =
        httpTransport.createRequestFactory(HttpCredentialsAdapter(credentials))
    private val objectMapper =
        jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
            configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true)
        }

    fun fetchUser(username: String): GoogleDirectoryUser {
        val url = GenericUrl("${properties.directoryApiBaseUrl}/users/$username")
        url["customer"] = properties.directoryCustomer

        val response = requestFactory.buildGetRequest(url).execute()
        return try {
            val body = response.parseAsString()
            val payload = objectMapper.readValue(body, UserResponse::class.java)
            GoogleDirectoryUser(
                id = payload.id,
                primaryEmail = payload.primaryEmail,
                fullName = payload.name?.fullName ?: payload.primaryEmail,
                givenName = payload.name?.givenName,
                familyName = payload.name?.familyName,
                thumbnailPhotoUrl = payload.thumbnailPhotoUrl,
            )
        } finally {
            response.disconnect()
        }
    }

    fun fetchGroups(userKey: String): Set<String> {
        val url = GenericUrl("${properties.directoryApiBaseUrl}/groups")
        url["userKey"] = userKey

        val response = requestFactory.buildGetRequest(url).execute()
        return try {
            val body = response.parseAsString()
            val payload = objectMapper.readValue(body, GroupsResponse::class.java)
            payload.groups
                ?.mapNotNull { it.email }
                ?.toSet()
                .orEmpty()
        } finally {
            response.disconnect()
        }
    }

    private fun loadCredentials(properties: GoogleLdapProperties): GoogleCredentials {
        val raw =
            FileInputStream(properties.serviceAccountJsonPath).use { stream ->
                GoogleCredentials.fromStream(stream)
            }
        val scoped = raw.createScoped(DIRECTORY_SCOPES)
        return properties.impersonatedUser?.let { scoped.createDelegated(it) } ?: scoped
    }

    data class UserResponse(
        val id: String = "",
        val primaryEmail: String = "",
        val name: UserName? = null,
        val thumbnailPhotoUrl: String? = null,
    )

    data class UserName(
        val fullName: String? = null,
        val givenName: String? = null,
        val familyName: String? = null,
    )

    data class GroupsResponse(
        val groups: List<GroupResponse>? = null,
    )

    data class GroupResponse(
        val email: String? = null,
    )

    data class GoogleDirectoryUser(
        val id: String,
        val primaryEmail: String,
        val fullName: String,
        val givenName: String?,
        val familyName: String?,
        val thumbnailPhotoUrl: String?,
    )

    companion object {
        private val DIRECTORY_SCOPES =
            listOf(
                "https://www.googleapis.com/auth/admin.directory.user.readonly",
                "https://www.googleapis.com/auth/admin.directory.group.readonly",
            )
    }
}
