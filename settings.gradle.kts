// SPDX-License-Identifier: Apache-2.0

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Dependency repositories are declared at the project level (in the cloudevents.kmp-library
// convention plugin), not centrally here. Centralized `dependencyResolutionManagement` would
// require a `repositoriesMode`, and every mode conflicts with the environment: PREFER_SETTINGS
// warns about the project-level repositories that the Kotlin JS/Wasm plugins and the user's
// global init script legitimately add; PREFER_PROJECT (the default) lets that init-script
// mavenLocal shadow mavenCentral. Keeping repositories at the project level avoids both.

plugins {
    // Settings-level plugins cannot consume the version catalog (`libs` is not available
    // in a settings `plugins {}` block), so this version is a literal. Keep it in sync with
    // the `gradlePreCommit` entry in gradle/libs.versions.toml, which is the reference source.
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.1.20"
}

gitHooks {
    commitMsg { conventionalCommits() }
    createHooks()
}

rootProject.name = "cloudevents-kotlin"

include(":core")
project(":core").name = "cloudevents-kotlin-core"
