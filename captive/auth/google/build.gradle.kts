group = "cz.grimir.wifimanager.captive.ldap.google"

dependencies {
    implementation(project(":captive:application"))
    implementation(project(":shared:core"))
    implementation(project(":shared:application"))

    implementation(libs.springContext)
    implementation(libs.springLdapCore)
    implementation(libs.springBootStarter)
    implementation(libs.bundles.jackson)
    implementation(libs.kotlinLogging)

    implementation(libs.googleAuthLibrary)
    implementation(libs.googleApiClient)
}
