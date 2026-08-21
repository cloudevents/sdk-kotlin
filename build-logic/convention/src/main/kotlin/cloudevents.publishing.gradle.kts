plugins {
    id("com.vanniktech.maven.publish")
}

// Maven Central (Central Portal) publishing for the KMP artifact matrix: the root module
// plus one Maven module per target, each with its own jar/klib, sources jar, javadoc jar,
// POM, and Gradle module metadata. Every file is GPG-signed.
//
// The single module-specific seam is the `cloudeventsPublishing {}` extension declared below.
// A module overrides GAV/POM identity through it; when an extension property is left unset it
// falls back to the core defaults, so a module that omits the extension publishes the exact
// same (byte-identical) GAV, POM name, and POM description as before this parameterization.
// The `mavenPublishing {}` block must not be overridden from a module build — the extension is
// the override seam.
//
// The variable identity fields (coordinates + POM name/description) are applied in
// `afterEvaluate` because the extension is configured by the *module's* build script body,
// which runs after this convention plugin's `mavenPublishing {}` block would have been
// evaluated eagerly. Reading the extension here would capture the null/`:: Core` fallback
// before the module could set it, leaking core's POM identity onto every other module. The
// static POM sub-blocks (inceptionYear/url/licenses/developers/organization/scm/issueManagement)
// and the publish/sign behavior are not extension-dependent and stay eager.
//
// Credentials are supplied only in CI via ORG_GRADLE_PROJECT_* environment variables and are
// never committed. Locally, `publishToMavenLocal` needs no credentials: the local build
// resolves to a SNAPSHOT version, and signing is not required for SNAPSHOTs.
val cloudeventsPublishing = extensions.create("cloudeventsPublishing", CloudeventsPublishingExtension::class)

mavenPublishing {
    // Central Portal is the default host in this plugin version (OSSRH is sunset). The
    // release workflow drives the actual upload+release via the publishAndReleaseToMavenCentral
    // task; SNAPSHOT builds and publishToMavenLocal work without any credentials.
    publishToMavenCentral()
    signAllPublications()

    pom {
        inceptionYear = "2026"
        url = "https://github.com/cloudevents/sdk-kotlin"

        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "octaviospain"
                name = "Octavio Calleya Garcia"
                url = "https://github.com/octaviospain"
            }
        }

        organization {
            name = "CloudEvents"
            url = "https://cloudevents.io"
        }

        scm {
            url = "https://github.com/cloudevents/sdk-kotlin"
            connection = "scm:git:git://github.com/cloudevents/sdk-kotlin.git"
            developerConnection = "scm:git:ssh://git@github.com/cloudevents/sdk-kotlin.git"
        }

        issueManagement {
            system = "GitHub"
            url = "https://github.com/cloudevents/sdk-kotlin/issues"
        }
    }
}

project.afterEvaluate {
    // Module names already carry the `cloudevents-kotlin-` prefix (settings.gradle.kts renames
    // `:core` -> `cloudevents-kotlin-core`, `:json` -> `cloudevents-kotlin-json`), so the
    // artifactId default is simply the project name — which for `:core` yields the unchanged
    // `cloudevents-kotlin-core` GAV.
    val artifactId = cloudeventsPublishing.artifactId ?: project.name
    val pomName = cloudeventsPublishing.pomName ?: "CloudEvents Kotlin SDK :: Core"
    val pomDescription = cloudeventsPublishing.pomDescription ?:
        "Idiomatic, type-safe Kotlin Multiplatform API to compose, validate, encode, and decode CloudEvents."

    mavenPublishing {
        coordinates("io.cloudevents", artifactId, version.toString())
        pom {
            name = pomName
            description = pomDescription
        }
    }
}

// Backing type for `cloudeventsPublishing { artifactId = ...; pomName = ...; pomDescription = ... }`.
// All three are nullable so an unset property falls back to the core defaults above.
open class CloudeventsPublishingExtension {
    var artifactId: String? = null
    var pomName: String? = null
    var pomDescription: String? = null
}
