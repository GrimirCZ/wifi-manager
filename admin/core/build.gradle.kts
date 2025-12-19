group = "cz.grimir.wifimanager.admin.core"

dependencies {
    implementation(project(":shared:core"))

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}