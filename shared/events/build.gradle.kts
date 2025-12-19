group = "cz.grimir.wifimanager.shared.events"

dependencies {
    implementation(project(":shared:core"))

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
