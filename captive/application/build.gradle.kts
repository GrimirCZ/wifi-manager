group = "cz.grimir.wifimanager.captive.application"

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:events"))
    implementation(project(":shared:util"))
    implementation(project(":shared:application"))

    implementation(project(":captive:core"))

    implementation(libs.springBootStarter)
    implementation(libs.bundles.springContextTx)
    implementation(libs.bundles.jackson)

    implementation(libs.kotlinLogging)
    implementation(libs.yauaa)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testImplementation(libs.bundles.testMockito)
    testRuntimeOnly(libs.bundles.testRuntime)
}

tasks.test {
    useJUnitPlatform()
}
