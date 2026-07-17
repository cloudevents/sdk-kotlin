import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
    id("io.gitlab.arturbosch.detekt")
}

configure<SpotlessExtension> {
    // Main production Kotlin: ktlint formatting plus the SPDX header, in a single format
    // so the steps converge. Test sources and Gradle build scripts (*.gradle.kts) are
    // intentionally exempt from the license header (see the kotlinGradle block below).
    kotlin {
        target("src/*Main/**/*.kt")
        targetExclude("**/build/**/*.kt")

        ktlint("1.6.0")
            .editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "intellij_idea",
                    "indent_size" to "4",
                    // Align ktlint's line-length ceiling with detekt's MaxLineLength (120) so the
                    // two gates agree: ktlint wraps an over-long signature/expression body instead
                    // of collapsing it onto a single line that detekt would then reject.
                    "max_line_length" to "120",
                ),
            )

        // The delimiter anchors on the first real code line (package / @file: / import) so
        // the // SPDX line is recognized as the header, not as content. A wrong delimiter that
        // matches the // line makes Spotless treat the header as missing and oscillate.
        licenseHeader("// SPDX-License-Identifier: Apache-2.0\n\n", "(^package |^@file:|^import )")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.6.0")
    }
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    autoCorrect = false
    // No baseline — strict mode: any finding is a build failure
    config.from(files("$rootDir/detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    // Do not override the per-task source: each detektMetadata<SourceSet> task must
    // analyze its own source set so no shared/platform source set escapes analysis.
    exclude { it.file.invariantSeparatorsPath.contains("/build/") }
    jvmTarget = "21"
    reports {
        html.required.set(true)
        xml.required.set(false)
        sarif.required.set(false)
    }
}

// Strict coverage: gate `check` on detekt for every shared and platform metadata
// source set (commonMain plus every intermediate/native/web set), not commonMain alone.
tasks.named("check") {
    dependsOn(tasks.matching { it.name.matches(Regex("detektMetadata.+Main")) })
}
