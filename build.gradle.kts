import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    alias(libs.plugins.spring.boot) apply false
    id("io.spring.dependency-management")
    id("cz.grimir.wifimanager.boot-only-if-app-main-class") apply false
    id("cz.grimir.wifimanager.tailwind-assets")
    id("com.diffplug.spotless")
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
    apply(plugin = "com.diffplug.spotless")

    dependencies {
        implementation(kotlin("reflect"))
        implementation("org.slf4j:slf4j-api")
    }

    kotlin {
        jvmToolchain(21)
    }

    tasks.withType<BootRun> {
        environment("PROJECT_ROOT", rootProject.projectDir.absolutePath)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.matching { it.name == "processResources" }.configureEach {
        dependsOn(rootProject.tasks.named("buildCss"))
    }

    spotless {
        kotlin {
            target("**/*.kt")
            ktlint()
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint()
        }
    }
}
