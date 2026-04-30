plugins {
    kotlin("plugin.jpa")
}

group = "cz.grimir.wifimanager.captive.persistence"

dependencies {
    implementation(project(":captive:core"))
    implementation(project(":captive:application"))

    implementation(project(":shared:core"))
    implementation(project(":shared:events"))

    runtimeOnly("org.postgresql:postgresql")

    implementation(libs.bundles.springDataJpa)
    implementation(libs.bundles.jackson)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testImplementation(libs.springBootStarterTest)
    testImplementation(libs.testcontainersJunitJupiter)
    testImplementation(libs.testcontainersPostgresql)
    testRuntimeOnly(libs.bundles.testRuntime)
}
