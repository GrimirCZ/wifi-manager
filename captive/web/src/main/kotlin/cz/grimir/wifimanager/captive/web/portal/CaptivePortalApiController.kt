package cz.grimir.wifimanager.captive.web.portal

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import cz.grimir.wifimanager.captive.web.security.support.ClientInfo
import cz.grimir.wifimanager.captive.web.security.support.CurrentClient
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

private val CAPTIVE_JSON_MEDIA_TYPE = MediaType.parseMediaType("application/captive+json")

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaptivePortalApiResponse(
    @param:JsonProperty("captive")
    val captive: Boolean,
    @param:JsonProperty("user-portal-url")
    val userPortalUrl: String,
    @param:JsonProperty("seconds-remaining")
    val secondsRemaining: Long? = null,
)

@RestController
class CaptivePortalApiController(
    private val clientAccessStatusService: CaptiveClientAccessStatusService,
    private val properties: CaptivePortalApiProperties,
) {
    @GetMapping("/captive/api")
    fun api(
        @CurrentClient
        clientInfo: ClientInfo,
    ): ResponseEntity<CaptivePortalApiResponse> {
        val status = clientAccessStatusService.resolve(clientInfo)
        val response =
            CaptivePortalApiResponse(
                captive = status.captive,
                userPortalUrl = "${properties.publicBaseUrl.trimEnd('/')}/captive",
                secondsRemaining = status.secondsRemaining,
            )

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache().cachePrivate())
            .header(HttpHeaders.CONTENT_TYPE, CAPTIVE_JSON_MEDIA_TYPE.toString())
            .body(response)
    }
}
