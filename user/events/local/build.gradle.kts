group = "cz.grimir.wifimanager.user.events.local"

dependencies {
    implementation(project(":user:application"))
    implementation(project(":shared:events"))
    implementation(libs.springContext)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
