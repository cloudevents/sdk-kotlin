import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("de.infix.testBalloon")
    // Core Gradle plugin — lets the KMP plugin generate publication metadata (POM,
    // Gradle module metadata) carrying the io.cloudevents coordinates. This gives every
    // library module publishable coordinates; the Maven Central upload and GPG signing are
    // layered on publishable modules by the separate cloudevents.publishing convention.
    `maven-publish`
}

group = "io.cloudevents"
// Inherit the git-tag-derived version resolved by axion-release on the root project.
version = rootProject.version

// Dependency repositories are declared per module (rather than centralized in settings) so
// resolution stays in the default PREFER_PROJECT mode without emitting settings-vs-project
// repository warnings. The Kotlin JS/Wasm plugins add their own content-filtered tool
// repositories on top of these.
repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    // Pin the JVM toolchain so compilation and test execution use JDK 21
    // regardless of the JDK running Gradle, matching the CI environment.
    jvmToolchain(21)

    // JVM
    jvm()

    // JavaScript — IR backend is the only supported backend in current Kotlin
    js {
        nodejs()
        // browser() omitted — Node-only; add later for browser test matrix
    }

    // WebAssembly JS
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    // Apple Tier 1 — compile only on macOS runners; link/test on macOS
    @Suppress("DEPRECATION")
    macosX64 {}
    macosArm64 {}
    iosArm64 {}
    iosX64 {}
    iosSimulatorArm64 {}

    // Linux Tier 2
    linuxX64()
    linuxArm64()

    // Windows
    mingwX64()

    // Default hierarchy template auto-applied in Kotlin 2.x:
    //   commonMain → nativeMain → linuxMain / mingwMain / appleMain → individual targets
    //   Do NOT call applyDefaultHierarchyTemplate() — it is auto-applied in 2.x

    sourceSets {
        commonMain.dependencies {
            // stdlib-only — no third-party runtime deps
        }
        commonTest.dependencies {
            implementation("de.infix.testBalloon:testBalloon-framework-core:1.0.1-K2.4.0")
            implementation(kotlin("test"))
        }
    }
}
