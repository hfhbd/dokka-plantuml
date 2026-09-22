plugins {
    id("jvm")
}

dokka {
    dependencies {
        dokkaPlugin(project())
    }
}

dependencies {
    implementation(libs.dokka.core)
    implementation(libs.dokka.base)
    implementation(libs.plantuml)
}

testing {
    suites {
        named("test", JvmTestSuite::class) {
            dependencies {
                implementation(libs.dokka.test.api)
                implementation(libs.dokka.base.test.utils)
                implementation(libs.dokka.analysis.kotlin.symbols)
            }
        }
    }
}
