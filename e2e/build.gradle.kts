group = "cz.grimir.wifimanager.e2e"

dependencies {
    testImplementation(project(":app"))
    testImplementation(project(":captive:application"))
    testImplementation(project(":captive:core"))

    testImplementation(libs.playwright)
    testImplementation(libs.springBootStarterDataJpa)
    testImplementation(libs.springBootStarterTest)
    testImplementation(libs.testcontainersCore)
    testImplementation(libs.testcontainersJunitJupiter)
    testImplementation(libs.testcontainersPostgresql)

    testRuntimeOnly("org.postgresql:postgresql")
}

tasks.withType<Test>().configureEach {
    systemProperty("wifimanager.root-dir", rootProject.projectDir.absolutePath)
}
