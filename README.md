# CloudEvents Kotlin SDK

**A CNCF project** — part of the [CloudEvents](https://cloudevents.io) ecosystem.

![Build](https://github.com/cloudevents/sdk-kotlin/actions/workflows/pr-checks.yml/badge.svg)

---

## What It Is

A Kotlin Multiplatform SDK for [CloudEvents](https://cloudevents.io), the CNCF-governed
specification for describing event data in a common way. The SDK provides a type-safe,
idiomatic Kotlin API for composing, validating, encoding, and decoding CloudEvents across
JVM, JavaScript, WebAssembly, and native platforms.

The `:core` module is a pure-Kotlin, stdlib-only library with no JVM-specific
dependencies — it compiles on every supported Kotlin Multiplatform target from a single
shared source set.

---

## Version Support Policy

This SDK supports CloudEvents **v1.0** (latest spec text 1.0.2, honoring 1.0.1
clarifications) and **v0.3** per the CloudEvents SDK support policy special case.

The CloudEvents SDK support policy requires each SDK to support the latest major spec
version (currently v1.0) and, while v1.0 is the latest, also v0.3. Within a major
version only the latest minor text is tracked — there is no runtime toggle between
v1.0.1 and v1.0.2; both carry the `"1.0"` specversion value.

For the authoritative policy definition, see:
[cloudevents/spec — SDK Requirements](https://github.com/cloudevents/spec/blob/main/cloudevents/SDK.md)

---

## Modules

| Module | Status | Description |
|--------|--------|-------------|
| `:core` | Active | CloudEvent data model, type system, attribute validation, and Message-SPI interfaces. Stdlib-only, no third-party runtime dependencies. |
| Format modules (JSON, Avro, Protobuf) | Planned | Encode and decode CloudEvents in each event format. |
| Protocol binding modules (HTTP, Kafka, AMQP, ...) | Planned | Transport-specific binary and structured content modes. |

---

## KMP Target Matrix

| Target | Platform | CI Status |
|--------|----------|-----------|
| `jvm` | Any (JDK 21+) | Tested |
| `js` (Node) | Node.js >= 18 | Tested |
| `wasmJs` (Node) | Node.js >= 18 | Tested |
| `linuxX64` | Linux x86-64 | Tested |
| `linuxArm64` | Linux ARM64 | Planned CI |
| `macosX64` | macOS x86-64 | Planned CI |
| `macosArm64` | macOS Apple Silicon | Planned CI |
| `iosArm64` | iOS device (ARM64) | Planned CI |
| `iosX64` | iOS Simulator (x86-64) | Planned CI |
| `iosSimulatorArm64` | iOS Simulator (Apple Silicon) | Planned CI |
| `mingwX64` | Windows x86-64 | Planned CI |

**"Tested"** targets run on `ubuntu-latest` in CI on every PR.
**"Planned CI"** targets compile on any host but require a macOS (Apple targets) or
Windows (`mingwX64`) runner for test execution; these are added in a future CI expansion.

---

## Installation

> **Note:** `cloudevents-kotlin-core` is not yet published to Maven Central.
> Until a release is available, add it as a project dependency from source.

The artifact coordinates when published will be:

```kotlin
// build.gradle.kts
dependencies {
    // cloudevents-kotlin-core is not yet published; add as a project dependency for now
    implementation("io.cloudevents:cloudevents-kotlin-core:<version>")
}
```

Group: `io.cloudevents`
Artifact: `cloudevents-kotlin-core`

---

## Contributing

Contributions are welcome. Before submitting a pull request, please read
[CONTRIBUTING.md](CONTRIBUTING.md) for the full workflow — covering branching,
DCO sign-off, Conventional Commits, build and test instructions, and the code review
process.

---

## Links

- [CONTRIBUTING.md](CONTRIBUTING.md) — contribution workflow and guidelines
- [CloudEvents specification](https://github.com/cloudevents/spec) — normative spec source
- [CloudEvents SDK requirements](https://github.com/cloudevents/spec/blob/main/cloudevents/SDK.md) — version-support policy and SDK conformance requirements
- [CloudEvents website](https://cloudevents.io) — overview and community resources

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).
