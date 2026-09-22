package io.github.hfhbd.dokka.plantuml

import net.sourceforge.plantuml.SourceStringReader
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.pages.ContentCodeBlock
import org.jetbrains.dokka.pages.ContentComposite
import org.jetbrains.dokka.pages.ContentEmbeddedResource
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentText
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

public class PlantumlPlugin : DokkaPlugin() {

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    // The html preprocessors are used by both the single module and the all modules
    // page generation, unlike CoreExtensions.pageTransformer, which is only used by
    // the single module generation.
    public val pageTransformer: Extension<PageTransformer, *, *> by extending {
        dokkaBase.htmlPreprocessors providing ::PlantumlPageTransformer
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}

public class PlantumlPageTransformer(
    private val context: DokkaContext,
) : PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode {
        val locationProvider = context
            .plugin<DokkaBase>()
            .querySingle { locationProviderFactory }
            .getLocationProvider(input)

        val generated = mutableMapOf<String, String>()

        return input.transformContentPagesTree { page ->
            val pathToRoot = locationProvider.pathToRoot(page)
            page.modified(content = page.content.replacePlantuml(generated, pathToRoot))
        }
    }

    // ContentComposite.transformChildren only transforms direct children, so this
    // has to recurse manually to also replace nested plantuml code blocks and to be
    // able to replace a ContentCodeBlock with a ContentEmbeddedResource, which
    // recursiveMapTransform/mapTransform don't support, as they require the
    // replacement to be of the same type as the original node.
    private fun ContentNode.replacePlantuml(
        generated: MutableMap<String, String>,
        pathToRoot: String,
    ): ContentNode {
        if (this is ContentCodeBlock && language.equals("plantuml", ignoreCase = true)) {
            val source = children
                .filterIsInstance<ContentText>()
                .joinToString("\n") { it.text }

            val resource = generated.getOrPut(source) {
                render(source)
            }

            return ContentEmbeddedResource(
                address = pathToRoot + resource,
                altText = "PlantUML diagram",
                dci = dci,
                sourceSets = sourceSets,
                style = style,
                extra = extra
            )
        }

        return if (this is ContentComposite) {
            transformChildren { it.replacePlantuml(generated, pathToRoot) }
        } else {
            this
        }
    }

    /**
     * Renders the diagram into the output directory and returns the path of the
     * image relative to the root of the output directory.
     */
    private fun render(source: String): String {
        val path = "$IMAGE_DIRECTORY/${sha256(source)}.png"

        val file = File(context.configuration.outputDir, path)
        file.parentFile.mkdirs()

        FileOutputStream(file).use { output ->
            checkNotNull(SourceStringReader(source).outputImage(output)) {
                "Failed to render PlantUML diagram"
            }
        }

        return path
    }

    private fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val IMAGE_DIRECTORY = "images/plantuml"
    }
}
