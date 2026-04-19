group = "cz.grimir.wifimanager.shared.application"

dependencies {
    implementation(project(":shared:core"))

    implementation(libs.springContext)
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(libs.kotlinLogging)
    implementation(libs.bundles.jackson)
    implementation(libs.googleAuthLibrary)
    implementation(libs.googleApiClient)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testImplementation(libs.bundles.testMockito)
    testRuntimeOnly(libs.bundles.testRuntime)
}
