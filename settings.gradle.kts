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
    // Run formatting + static analysis before every commit. spotlessApply auto-fixes
    // formatting, then we re-stage only the files that were already staged so the fix
    // lands in the same commit without pulling in unrelated working-tree changes.
    // detektMetadataMain analyzes commonMain, where the model and validation code lives;
    // a detekt finding (non-zero exit) aborts the commit. The full multi-source-set detekt
    // and all-target tests remain gated by `gradle check` and CI.
    preCommit {
        from {
            """
            staged=${'$'}(git diff --cached --name-only --diff-filter=ACM -- '*.kt' '*.kts')
            if [ -z "${'$'}staged" ]; then
                ./gradlew detektMetadataMain --quiet || exit 1
                exit 0
            fi
            # spotlessApply formats the whole working tree, so stash everything that is
            # not staged first. This keeps unrelated unstaged edits (partially staged
            # hunks and other files) out of the commit. --keep-index leaves the staged
            # snapshot in place to be formatted; the stash is restored afterwards.
            stashed=0
            if ! git diff --quiet || [ -n "${'$'}(git ls-files --others --exclude-standard)" ]; then
                git stash push --quiet --keep-index --include-untracked --message pre-commit-format && stashed=1
            fi
            ./gradlew spotlessApply detektMetadataMain --quiet
            status=${'$'}?
            if [ "${'$'}status" -eq 0 ]; then
                printf '%s\n' "${'$'}staged" | while IFS= read -r f; do
                    [ -f "${'$'}f" ] && git add -- "${'$'}f"
                done
            fi
            if [ "${'$'}stashed" -eq 1 ]; then
                git stash pop --quiet || echo "pre-commit: unstaged changes left in the stash; restore with 'git stash pop'"
            fi
            exit ${'$'}status
            """.trimIndent()
        }
    }
    commitMsg { conventionalCommits() }
    createHooks()
}

rootProject.name = "cloudevents-kotlin"

include(":core")
project(":core").name = "cloudevents-kotlin-core"
