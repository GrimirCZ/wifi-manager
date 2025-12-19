group = "cz.grimir.wifimanager.shared.core"

dependencies {
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
