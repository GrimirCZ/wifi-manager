package cz.grimir.wifimanager.admin.application.exceptions

class UserWithThisEmailAlreadyExistsForAnotherProvider(
    val email: String,
) : RuntimeException("User with email '$email' already exists for another provider")

