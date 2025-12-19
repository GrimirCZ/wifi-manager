group = "cz.grimir.wifimanager.admin.events.local"

dependencies {
    implementation(project(":admin:application"))
    implementation(project(":shared:events"))
    implementation(libs.springContext)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
