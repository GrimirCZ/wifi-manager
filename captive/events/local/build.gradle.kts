group = "cz.grimir.wifimanager.captive.events.local"

dependencies {
    implementation(project(":captive:application"))
    implementation(project(":shared:events"))
    implementation(libs.springContext)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
