plugins {
    id("com.vanniktech.maven.publish")
}

// Maven Central (Central Portal) publishing for the KMP artifact matrix: the root module
// plus one Maven module per target, each with its own jar/klib, sources jar, javadoc jar,
// POM, and Gradle module metadata. Every file is GPG-signed.
//
// Credentials are supplied only in CI via ORG_GRADLE_PROJECT_* environment variables and are
// never committed. Locally, `publishToMavenLocal` needs no credentials: the local build
// resolves to a SNAPSHOT version, and signing is not required for SNAPSHOTs.
mavenPublishing {
    // Central Portal is the default host in this plugin version (OSSRH is sunset). The
    // release workflow drives the actual upload+release via the publishAndReleaseToMavenCentral
    // task; SNAPSHOT builds and publishToMavenLocal work without any credentials.
    publishToMavenCentral()
    signAllPublications()

    coordinates("io.cloudevents", "cloudevents-kotlin-core", version.toString())

    pom {
        name = "CloudEvents Kotlin SDK :: Core"
        description = "Idiomatic, type-safe Kotlin Multiplatform API to compose, validate, " +
            "encode, and decode CloudEvents."
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
