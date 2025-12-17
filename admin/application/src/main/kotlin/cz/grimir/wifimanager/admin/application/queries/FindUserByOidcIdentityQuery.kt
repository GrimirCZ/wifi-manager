package cz.grimir.wifimanager.admin.application.queries

data class FindUserByOidcIdentityQuery(
    val issuer: String,
    val subject: String,
)
