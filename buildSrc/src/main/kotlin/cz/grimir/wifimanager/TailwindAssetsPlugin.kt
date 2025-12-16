package cz.grimir.wifimanager

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register

class TailwindAssetsPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {

        val adminOutDir = layout.projectDirectory.dir("admin/web/src/main/resources/static/assets/admin")
        adminOutDir.asFile.mkdirs()
        val captiveOutDir = layout.projectDirectory.dir("captive/web/src/main/resources/static/assets/captive")
        captiveOutDir.asFile.mkdirs()

        tasks.register<Exec>("buildCss") {
            group = "assets"
            description = "Build Tailwind CSS for admin and captive modules."

            workingDir = layout.projectDirectory.asFile
            commandLine(bunCommand(), "run", "build:css")

            inputs.files(
                layout.projectDirectory.file("package.json"),
                layout.projectDirectory.file("postcss.config.js"),
                layout.projectDirectory.file("shared/ui/tailwind/preset.js"),
                layout.projectDirectory.file("shared/ui/tailwind/shared.css"),
                layout.projectDirectory.file("admin/web/tailwind.config.js"),
                layout.projectDirectory.file("admin/web/src/main/frontend/admin.css"),
                layout.projectDirectory.file("captive/web/tailwind.config.js"),
                layout.projectDirectory.file("captive/web/src/main/frontend/captive.css"),
            )

            inputs.files(layout.projectDirectory.asFileTree.matching {
                include("admin/web/src/main/resources/templates/**/*.html")
                include("captive/web/src/main/resources/templates/**/*.html")
                include("shared/ui/src/main/resources/templates/**/*.html")
                exclude("**/node_modules/**")
            })

            outputs.files(
                layout.projectDirectory.file("admin/web/src/main/resources/static/assets/admin/admin.css"),
                layout.projectDirectory.file("captive/web/src/main/resources/static/assets/captive/captive.css"),
            )
        }

        tasks.register<Exec>("watchCss") {
            group = "assets"
            description = "Watch Tailwind CSS builds for admin and captive modules."

            workingDir = layout.projectDirectory.asFile
            commandLine(bunCommand(), "run", "watch:css")

            doFirst {
                adminOutDir.asFile.mkdirs()
                captiveOutDir.asFile.mkdirs()
            }
        }
    }

    private fun bunCommand(): String {
        val os = System.getProperty("os.name")
        return if (os.startsWith("Windows")) "bun.exe" else "bun"
    }
}