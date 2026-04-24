group = "cz.grimir.wifimanager.shared.security"

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:application"))

    implementation(libs.springBootStarterSecurity)
    implementation(libs.springBootStarterOauth2Client)
    implementation(libs.springSecurityOauth2Jose)
    implementation(libs.springBootStarterWeb)
    implementation(libs.springSecurityWeb)
    implementation(libs.htmxSpringBoot)
    implementation(libs.kotlinCoroutinesCore)
    implementation(libs.kotlinLogging)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testImplementation(libs.bundles.testMockito)
    testImplementation(libs.springBootStarterTest)
    testRuntimeOnly(libs.bundles.testRuntime)
}
