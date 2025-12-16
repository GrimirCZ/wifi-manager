plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    id("cz.grimir.wifimanager.boot-only-if-app-main-class") apply false
    kotlin("jvm")
    kotlin("plugin.spring")
}

allprojects {
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "cz.grimir.wifimanager.boot-only-if-app-main-class")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")

    dependencies {
        implementation(kotlin("reflect"))
    }

    kotlin {
        jvmToolchain(21)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}