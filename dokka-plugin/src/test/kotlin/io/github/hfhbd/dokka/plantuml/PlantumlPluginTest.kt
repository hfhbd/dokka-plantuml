package io.github.hfhbd.dokka.plantuml

import utils.TestOutputWriterPlugin
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlantumlPluginTest : BaseAbstractTest() {
    @Test
    fun simpleTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                    includes = listOf("f.md")
                }
            }
        }

        val source =
            """
        /src/main/kotlin/test/Test.kt
        val answer = 42

        /f.md
        # Module root

        ```plantuml
        @startuml
        Bob -> Alice : hello
        @enduml
        ```
        """.trimIndent()

        val writerPlugin = TestOutputWriterPlugin(true)

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(
                writerPlugin, PlantumlPlugin(),
            )
        ) {
            renderingStage = { _, context ->
                val moduleIndex = writerPlugin.writer.contents.getValue("index.html")

                assertFalse(
                    moduleIndex.contains("startuml"),
                    "The raw plantuml source should have been replaced by a rendered image"
                )

                val imgTag = requireNotNull(
                    Regex("""<img alt="PlantUML diagram" src="(images/plantuml/[^"]+\.png)">""").find(moduleIndex)
                ) { "Expected a rendered PlantUML <img> tag in the module page" }

                // The image is written relative to the output directory, so it is part of the
                // generated documentation and stays valid if the module output is relocated.
                val renderedFile = context.configuration.outputDir.resolve(imgTag.groupValues[1])
                assertTrue(renderedFile.exists(), "Rendered PlantUML PNG file should exist on disk")
                assertTrue(renderedFile.length() > 0, "Rendered PlantUML PNG file should not be empty")

                val pngMagicBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
                val actualBytes = renderedFile.readBytes().copyOfRange(0, 4)
                assertTrue(actualBytes.contentEquals(pngMagicBytes), "Rendered file should be a valid PNG image")
            }
        }
    }
}
