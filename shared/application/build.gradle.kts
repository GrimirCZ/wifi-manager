group = "cz.grimir.wifimanager.shared.application"

dependencies {
    implementation(project(":shared:core"))

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
