import org.gradle.api.tasks.testing.Test

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

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("screenshots")
    }
}

tasks.register<Test>("screenshotTest") {
    group = "verification"
    description = "Runs the dedicated Playwright screenshot suite."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("screenshots")
    }
    shouldRunAfter(tasks.named("test"))
    systemProperty("wifimanager.root-dir", rootProject.projectDir.absolutePath)
    systemProperty(
        "wifimanager.screenshots.output-dir",
        rootProject.layout.buildDirectory.dir("reports/screenshots").get().asFile.absolutePath,
    )
}
