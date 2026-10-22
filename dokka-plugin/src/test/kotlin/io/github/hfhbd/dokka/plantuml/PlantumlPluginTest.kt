package io.github.hfhbd.dokka.plantuml

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import utils.TestOutputWriterPlugin
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlantumlPluginTest : BaseAbstractTest() {
    @Test
    fun simpleTest() {
        val configuration = dokkaConfiguration {
            pluginsConfigurations.add(
                PluginConfigurationImpl(
                    fqPluginName = PlantumlPlugin::class.qualifiedName!!,
                    serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
                    values = "{}"
                )
            )

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

                val renderedFile = context.configuration.outputDir.resolve(imgTag.groupValues[1])
                assertTrue(renderedFile.exists(), "Rendered PlantUML PNG file should exist on disk")
                assertContentEquals(PlantumlPluginTest::class.java.getResource("/simpleTest.png").readBytes(), renderedFile.readBytes())
            }
        }
    }

    @Test
    fun includeTest() {
        val out = File("build/dokka-tests/include-test")
        out.deleteRecursively()

        val input = File("src/test/resources/include-test")
        out.parentFile.mkdirs()
        input.copyRecursively(out)

        val configuration = dokkaConfiguration {
            outputDir = out
            pluginsConfigurations.add(
                PluginConfigurationImpl(
                    fqPluginName = PlantumlPlugin::class.qualifiedName!!,
                    serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
                    values = """{"workingDirectory":"build/dokka-tests/include-test"}"""
                )
            )
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/test/resources/include-test/src/main/kotlin/test/Test.kt")
                    includes = listOf("src/test/resources/include-test/f.md")
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin(true)

        testFromData(
            configuration,
            useOutputLocationFromConfig = true,
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
                assertContentEquals(PlantumlPluginTest::class.java.getResource("/include-test/expected.png").readBytes(), renderedFile.readBytes())
            }
        }
    }
}
