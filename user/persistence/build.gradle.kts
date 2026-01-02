group = "cz.grimir.wifimanager.user.persistence"

dependencies {
    implementation(project(":user:application"))
    implementation(project(":shared:core"))

    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testImplementation(libs.testcontainersJunitJupiter)
    testImplementation(libs.testcontainersPostgresql)
    testRuntimeOnly(libs.bundles.testRuntime)
}
