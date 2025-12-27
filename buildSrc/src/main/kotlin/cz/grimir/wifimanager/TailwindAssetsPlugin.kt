package cz.grimir.wifimanager

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.register
import java.io.File

class TailwindAssetsPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {

        val adminOutDir = layout.projectDirectory.dir("admin/web/src/main/resources/static/assets/admin")
        adminOutDir.asFile.mkdirs()
        val captiveOutDir = layout.projectDirectory.dir("captive/web/src/main/resources/static/assets/captive")
        captiveOutDir.asFile.mkdirs()

        val tablerOutDir = layout.projectDirectory.dir("shared/ui/src/main/resources/static/assets/shared/icons")

        val syncTablerIcons =
            tasks.register<Sync>("syncTablerIcons") {
                group = "assets"
                description = "Sync Tabler SVG icons from node_modules into shared/ui static assets."

                val tablerIconsDir = layout.projectDirectory.dir("node_modules/@tabler/icons/icons")
                from(tablerIconsDir) {
                    include("**/*.svg")
                }
                into(tablerOutDir)

                inputs.dir(tablerIconsDir)
                outputs.dir(tablerOutDir)

                onlyIf {
                    tablerIconsDir.asFile.exists()
                }
            }

        tasks.register<Exec>("buildCss") {
            group = "assets"
            description = "Build Tailwind CSS for admin and captive modules."

            dependsOn(syncTablerIcons)

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
                tablerOutDir.asFile.mkdirs()
            }
        }
    }

    private fun bunCommand(): String {
        val cmd = if (System.getProperty("os.name").startsWith("Windows")) "bun.exe" else "bun"

        val file = File(cmd)
        if (file.exists() && file.canExecute()) return file.absolutePath

        val pathEnv = System.getenv("PATH") ?: ""
        val pathSeparator = if (System.getProperty("os.name").startsWith("Windows")) ";" else ":"

        val executable = pathEnv.split(pathSeparator)
            .map { File(it, cmd) }
            .find { it.exists() && it.canExecute() }

        if (executable != null) {
            return executable.absolutePath
        }

        val homeBun = File(System.getProperty("user.home"), ".bun/bin/$cmd")
        if (homeBun.exists()) return homeBun.absolutePath

        return cmd
    }
}
