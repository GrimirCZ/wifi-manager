dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    // Use the Foojay Toolchains plugin to automatically download JDKs required by subprojects.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"

    id("org.springframework.boot") version "4.0.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.spring") version "2.0.21" apply false
    kotlin("plugin.jpa") version "2.0.21" apply false
}

include("shared")
include("shared:events")

include("admin:core")
include("admin:application")
include("admin:web")
include("admin:persistence")
include("admin:events:local")

include("captive:core")
include("captive:application")
include("captive:web")
include("captive:persistence")
include("captive:events:local")

include(":app")

rootProject.name = "wifimanager"
include("shared:events")
