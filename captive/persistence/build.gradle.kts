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

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
