package cz.grimir.wifimanager

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType

class BootOnlyWithMainClassPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        pluginManager.apply("base")
        pluginManager.apply("org.springframework.boot")

        extensions.configure<BasePluginExtension> {
            archivesName.set(
                "${rootProject.name}-${project.path.trimStart(':').replace(':', '-')}"
            )
        }

        // Disable by default (works even if tasks are created later)
        tasks.matching { it.name == "bootJar" }.configureEach {
            enabled = false
        }
        tasks.matching { it.name == "bootRun" }.configureEach { enabled = false }

        // Decide late, after the module build script sets application.mainClass
        afterEvaluate {
            val appExt = extensions.findByType<JavaApplication>()
            val main = appExt?.mainClass?.orNull?.trim()

            val hasMain = !main.isNullOrEmpty()

            tasks.matching { it.name == "bootJar" }.configureEach { enabled = hasMain }
            tasks.matching { it.name == "bootRun" }.configureEach { enabled = hasMain }
        }
    }
}

