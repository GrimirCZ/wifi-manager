group = "cz.grimir.wifimanager.captive.web"

dependencies {
    implementation(project(":captive:application"))

    implementation(libs.bundles.springWebUi)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
