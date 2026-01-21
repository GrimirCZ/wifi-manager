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
    kotlin("jvm") version "2.2.20" apply false
    kotlin("plugin.spring") version "2.2.20" apply false
    kotlin("plugin.jpa") version "2.2.20" apply false

    id("com.diffplug.spotless") version "8.1.0" apply false
}

include("shared:core")
include("shared:application")
include("shared:events")
include("shared:security")
include("shared:ui")
include("shared:util")

include("admin:core")
include("admin:application")
include("admin:web")
include("admin:persistence")
include("admin:scheduler")
include("admin:events:local")

include("user:application")
include("user:persistence")
include("user:events:local")

include("captive:core")
include("captive:application")
include("captive:routeragent")
include("captive:web")
include("captive:persistence")
include("captive:events:local")
include("captive:auth:google")
include("captive:auth:keycloak")

include(":app")

rootProject.name = "wifimanager"
include("shared:events")
