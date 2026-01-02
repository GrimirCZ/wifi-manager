group = "cz.grimir.wifimanager.admin.application"

val mockitoAgent by configurations.creating

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:application"))
    implementation(project(":shared:events"))
    implementation(project(":admin:core"))

    implementation("org.slf4j:slf4j-api")
    implementation(libs.bundles.springContextTx)

    implementation(libs.kotlinLogging)

    mockitoAgent(libs.mockitoAgent)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.bundles.testMockito)
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
