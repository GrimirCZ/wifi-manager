group = "cz.grimir.wifimanager.captive.auth.keycloak"

dependencies {
    implementation(project(":captive:application"))
    implementation(project(":shared:core"))
    implementation(project(":shared:application"))

    implementation(libs.springContext)
    implementation(libs.springBootStarter)
    implementation(libs.springBootStarterWeb)
    implementation(libs.bundles.jackson)
    implementation(libs.kotlinLogging)
}
