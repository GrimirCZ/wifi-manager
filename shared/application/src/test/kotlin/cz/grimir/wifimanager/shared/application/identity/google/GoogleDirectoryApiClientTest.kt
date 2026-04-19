package cz.grimir.wifimanager.shared.application.identity.google

import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.http.HttpResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class GoogleDirectoryApiClientTest {
    private val properties =
        GoogleDirectoryApiProperties(
            directoryCustomer = "customer-1",
            serviceAccountJsonPath = "/tmp/google.json",
            impersonatedUser = "admin@example.com",
            directoryApiBaseUrl = "https://directory.example.test",
        )

    @Test
    fun `fetchUser requests user endpoint with provided user key`() {
        val requestFactory = mock<HttpRequestFactory>()
        val request = mock<HttpRequest>()
        val response = mock<HttpResponse>()
        given(requestFactory.buildGetRequest(org.mockito.kotlin.any())).willReturn(request)
        given(request.execute()).willReturn(response)
        given(response.parseAsString()).willReturn(
            """
            {
              "id": "user-123",
              "primaryEmail": "alice@example.com",
              "name": {"fullName": "Alice Doe"},
              "thumbnailPhotoUrl": "https://example.com/avatar.png"
            }
            """.trimIndent(),
        )
        val client = GoogleDirectoryApiClient(properties, { requestFactory })

        val result = client.fetchUser("google-sub-123")

        val urlCaptor = argumentCaptor<GenericUrl>()
        verify(requestFactory).buildGetRequest(urlCaptor.capture())
        assertEquals(true, urlCaptor.firstValue.build().startsWith("https://directory.example.test/users/google-sub-123"))
        assertEquals("customer-1", urlCaptor.firstValue["customer"])
        assertEquals("user-123", result.id)
        assertEquals("alice@example.com", result.primaryEmail)
        verify(response).disconnect()
    }

    @Test
    fun `fetchGroups requests groups endpoint with provided user key`() {
        val requestFactory = mock<HttpRequestFactory>()
        val request = mock<HttpRequest>()
        val response = mock<HttpResponse>()
        given(requestFactory.buildGetRequest(org.mockito.kotlin.any())).willReturn(request)
        given(request.execute()).willReturn(response)
        given(response.parseAsString()).willReturn(
            """
            {
              "groups": [
                {"email": "wifi-admins@example.com"},
                {"email": "wifi-staff@example.com"}
              ]
            }
            """.trimIndent(),
        )
        val client = GoogleDirectoryApiClient(properties, { requestFactory })

        val result = client.fetchGroups("google-sub-123")

        val urlCaptor = argumentCaptor<GenericUrl>()
        verify(requestFactory).buildGetRequest(urlCaptor.capture())
        assertEquals("https://directory.example.test/groups?userKey=google-sub-123", urlCaptor.firstValue.build())
        assertEquals(setOf("wifi-admins@example.com", "wifi-staff@example.com"), result)
        verify(response).disconnect()
    }
}
