import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
    id("io.github.hfhbd.mavencentral")
    id("java-test-fixtures")
}

kotlin {
    jvmToolchain(17)
    explicitApi()

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
}

dokka {
    val module = project.name
    dokkaSourceSets.configureEach {
        reportUndocumented = true
        includes.from("README.md")
        val sourceSetName = name
        File("$module/src/$sourceSetName").takeIf { it.exists() }?.let {
            sourceLink {
                localDirectory = file("src/$sourceSetName/kotlin")
                remoteUrl = uri("https://github.com/hfhbd/validation/tree/main/$module/src/$sourceSetName/kotlin")
                remoteLineSuffix = "#L"
            }
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

testing.suites.withType(JvmTestSuite::class) {
    useKotlinTest()
}

publishing {
    publications.register<MavenPublication>("mavenJava") {
        from(components["java"])
    }

    repositories {
        maven(url = "https://maven.pkg.github.com/hfhbd/validation") {
            name = "GitHubPackages"
            credentials(PasswordCredentials::class)
        }
    }

    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "hfhbd Validation"
            description = "Validation"
            url = "https://github.com/hfhbd/validation"
            licenses {
                license {
                    name = "Apache-2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "hfhbd"
                    name = "Philip Wedemann"
                    email = "mybztg+mavencentral@icloud.com"
                }
            }
            scm {
                connection = "scm:git://github.com/hfhbd/validation.git"
                developerConnection = "scm:git://github.com/hfhbd/validation.git"
                url = "https://github.com/hfhbd/validation"
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        providers.gradleProperty("signingKey").orNull,
        providers.gradleProperty("signingPassword").orNull,
    )
    isRequired = providers.gradleProperty("signingKey").isPresent
    sign(publishing.publications)
}

// https://youtrack.jetbrains.com/issue/KT-46466
val signingTasks = tasks.withType<Sign>()
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(signingTasks)
}
