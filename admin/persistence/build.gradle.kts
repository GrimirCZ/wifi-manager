plugins {
    kotlin("plugin.jpa")
}

group = "cz.grimir.wifimanager.admin.persistence"

dependencies {
    implementation(project(":admin:core"))
    implementation(project(":admin:application"))

    implementation(project(":shared:core"))

    runtimeOnly("org.postgresql:postgresql")

    implementation(libs.bundles.springDataJpa)
//    implementation("io.hypersistence:hypersistence-utils-hibernate-71:3.14.0")

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testBase)
    testRuntimeOnly(libs.bundles.testRuntime)
}
