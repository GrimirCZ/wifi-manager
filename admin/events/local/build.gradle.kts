group = "cz.grimir.wifimanager.admin.events.local"

dependencies {
    implementation(project(":admin:application"))
    implementation(project(":admin:persistence"))
    implementation(project(":shared:events"))
    implementation(project(":shared:core"))
    implementation(libs.springContext)
    implementation(libs.springTx)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
