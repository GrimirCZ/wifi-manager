group = "cz.grimir.wifimanager.captive.events.local"

dependencies {
    implementation(project(":captive:application"))
    implementation(project(":captive:persistence"))
    implementation(project(":shared:events"))
    implementation(project(":shared:core"))
    implementation(libs.springContext)
    implementation(libs.springTx)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
