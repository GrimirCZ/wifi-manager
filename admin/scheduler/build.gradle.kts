group = "cz.grimir.wifimanager.admin.scheduler"

dependencies {
    implementation(project(":admin:application"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-jdbc")

    implementation(libs.shedlock.spring)
    implementation(libs.shedlock.provider.jdbc.template)

    implementation(libs.kotlinLogging)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
