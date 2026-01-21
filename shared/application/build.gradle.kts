group = "cz.grimir.wifimanager.shared.application"

dependencies {
    implementation(project(":shared:core"))

    implementation(libs.kotlinLogging)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
