group = "cz.grimir.wifimanager.admin.web"

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:ui"))
    implementation(project(":admin:core"))
    implementation(project(":admin:application"))

    implementation(libs.bundles.springWebUi)
    implementation(libs.springBootStarterOauth2Client)
    implementation(libs.springSecurityOauth2Jose)
    implementation(libs.bundles.htmx)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
