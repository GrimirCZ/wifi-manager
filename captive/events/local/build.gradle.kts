group = "cz.grimir.wifimanager.captive.events.local"

dependencies {
    implementation(project(":captive:application"))
    implementation(project(":captive:core"))
    implementation(project(":captive:persistence"))
    implementation(project(":shared:application"))
    implementation(project(":shared:events"))
    implementation(project(":shared:core"))
    implementation(libs.springContext)
    implementation(libs.springTx)
    implementation(libs.kotlinLogging)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testImplementation(libs.bundles.testMockito)
    testImplementation(libs.bundles.jackson)
    testRuntimeOnly(libs.bundles.testRuntime)
}
