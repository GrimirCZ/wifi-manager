package cz.grimir.wifimanager.admin.application.identity.query

data class FindUserByOidcIdentityQuery(
    val issuer: String,
    val subject: String,
)
