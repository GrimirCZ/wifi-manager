group = "cz.grimir.wifimanager.user.application"

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:events"))

    implementation(libs.bundles.springContextTx)
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
