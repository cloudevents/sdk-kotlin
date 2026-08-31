// SPDX-License-Identifier: Apache-2.0

plugins {
    id("cloudevents.kmp-library")
    id("cloudevents.quality")
    id("cloudevents.publishing")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.cyclonedx)
}

// CycloneDX SBOM. The plugin resolves a JVM-style runtime classpath, so it is scoped to the
// KMP JVM target's runtime configuration — the conventional, pragmatic scope for a multiplatform
// library (the same JVM-target-measured convention used for coverage). Native/JS/Wasm klib
// dependencies are therefore not represented; per-target SBOMs are future work. The output is
// named bom.cdx.json so OSV-Scanner auto-detects the CycloneDX format from the extension.
val sbomFile = layout.buildDirectory.file("reports/cyclonedx/bom.cdx.json")
tasks.cyclonedxDirectBom {
    projectType = org.cyclonedx.model.Component.Type.LIBRARY
    includeConfigs = listOf("jvmRuntimeClasspath")
    jsonOutput = sbomFile.get().asFile
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The protobuf module consumes :core's type system and data-origin seam, exactly like
            // :cloudevents-kotlin-json. Referenced by its settings-renamed path.
            implementation(project(":cloudevents-kotlin-core"))
            // `implementation`, never `api`: no ProtoBuf/KSerializer type leaks into the public
            // surface. The serialization compiler plugin stays scoped to this module only.
            implementation(libs.kotlinx.serialization.protobuf)
        }
    }
}

// Publish under this module's own coordinates/POM instead of the core defaults. The extension
// (cloudevents.publishing) is the single override seam — never override `mavenPublishing {}`
// here, or the core POM strings would risk leaking onto this module's POM.
cloudeventsPublishing {
    artifactId = "cloudevents-kotlin-protobuf"
    pomName = "CloudEvents Kotlin SDK :: Protobuf Event Format"
    pomDescription = "CloudEvents protobuf event format (structured + batch) for the Kotlin SDK"
}
