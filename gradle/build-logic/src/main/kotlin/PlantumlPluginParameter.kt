import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.Input
import org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import javax.inject.Inject

@OptIn(InternalDokkaGradlePluginApi::class)
abstract class PlantumlPluginParameter @Inject constructor(
    name: String,
    layout: ProjectLayout,
) : DokkaPluginParametersBaseSpec(
    name,
    "io.github.hfhbd.dokka.plantuml.PlantumlPlugin",
) {
    @get:Input
    val projectDirectory: String = layout.projectDirectory.asFile.absolutePath

    override fun jsonEncode(): String {
        return """
            {
              "workingDirectory": "$projectDirectory"
            }
            """
    }
}
