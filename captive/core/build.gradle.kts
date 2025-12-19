group = "cz.grimir.wifimanager.captive.core"

dependencies {
    implementation(project(":shared:core"))

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}