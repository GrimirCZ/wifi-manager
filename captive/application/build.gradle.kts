group = "cz.grimir.wifimanager.captive.application"

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:events"))
    implementation(project(":shared:util"))
    implementation(project(":shared:application"))

    implementation(project(":captive:core"))

    implementation(libs.bundles.springContextTx)

    implementation(libs.kotlinLogging)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}

tasks.test {
    useJUnitPlatform()
}
