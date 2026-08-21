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
            // The JSON module consumes :core's type system (and later its message-SPI seam).
            // Referenced by its settings-renamed path (cloudevents.kmp-library honors `project(":core")
            // only as `:cloudevents-kotlin-core` after the settings.gradle.kts rename).
            implementation(project(":cloudevents-kotlin-core"))
            // `implementation`, never `api`: no Json/KSerializer type leaks into the public
            // surface. The serialization compiler plugin stays scoped to this module only.
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
