group = "cz.grimir.wifimanager.captive.web"

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:ui"))
    implementation(project(":captive:core"))
    implementation(project(":captive:application"))

    implementation(libs.bundles.springWebUi)
    implementation(libs.bundles.htmx)
    implementation(libs.kotlinLogging)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
