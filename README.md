# CloudEvents Kotlin SDK

**A CNCF project** — part of the [CloudEvents](https://cloudevents.io) ecosystem.

![Maven Central Version](https://img.shields.io/maven-central/v/io.cloudevents/cloudevents-kotlin-core)
![Build](https://img.shields.io/github/actions/workflow/status/cloudevents/sdk-kotlin/.github%2Fworkflows%2Fbuild.yml?logo=github)

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

## Feature Support

Support for the CloudEvents specification by version, and the Kotlin Multiplatform
targets each feature runs on. The version columns mirror the Kotlin column of the upstream
[SDK feature matrix](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/SDK.md#feature-support);
the **Platforms** column is specific to this SDK. Because `:core` is pure `commonMain`,
every feature it provides is available on **all** supported targets.

|                        | [v0.3](https://github.com/cloudevents/spec/tree/v0.3) | [v1.0](https://github.com/cloudevents/spec/tree/v1.0) | Platforms |
|:-----------------------|:-----------------------------------------------------:|:-----------------------------------------------------:|:----------|
| CloudEvents Core       | :heavy_check_mark:<sup>†</sup>                        | :heavy_check_mark:                                    | All targets<sup>‡</sup> |
| JSON Event Format      | :heavy_check_mark:<sup>§</sup>                        | :heavy_check_mark:                                    | All targets<sup>‡</sup> |
| Avro Event Format      | :x:                                                   | :x:                                                   | —         |
| Protobuf Event Format  | :x:                                                   | :x:                                                   | —         |
| HTTP Protocol Binding  | :x:                                                   | :x:                                                   | —         |
| Kafka Protocol Binding | :x:                                                   | :x:                                                   | —         |
| AMQP Protocol Binding  | :x:                                                   | :x:                                                   | —         |
| MQTT Protocol Binding  | :x:                                                   | :x:                                                   | —         |
| NATS Protocol Binding  | :x:                                                   | :x:                                                   | —         |

:heavy_check_mark: supported &nbsp;•&nbsp; :x: not yet implemented

<sub>† v0.3 core is fully supported — the v0.3 attribute set (`schemaurl`, `datacontentencoding`)
with version-aware validation and naming rules. The v0.3 `Map`/`Any` attribute value types, whose
canonical encoding is JSON, are not part of this SDK's core model, which uses the same
type-system hierarchy for both versions.</sub>
<br>
<sub>‡ The shared `commonMain` source compiles to an artifact for every target in the
[KMP Target Matrix](#kmp-target-matrix) below. Its test suite currently *runs* on JVM,
JS (Node), Wasm/JS, and `linuxX64`; the Apple and Windows targets compile but execute
their tests only on macOS/Windows CI runners (marked "Planned CI").</sub>
<br>
<sub>§ The JSON event format (`:cloudevents-kotlin-json`) supports structured-mode
`application/cloudevents+json` and batch-mode `application/cloudevents-batch+json` for
both v1.0 and v0.3 — see [JSON Event Format](#json-event-format) below.</sub>

---

## Modules

| Module | Status | Description |
|--------|--------|-------------|
| `:core` | Active | CloudEvent data model, type system, attribute validation, and Message-SPI interfaces. Stdlib-only, no third-party runtime dependencies. |
| `:cloudevents-kotlin-json` | Active | JSON event format: encode/decode structured-mode `application/cloudevents+json` and batch-mode `application/cloudevents-batch+json` for v1.0 and v0.3. Uses kotlinx-serialization. |
| Format modules (Avro, Protobuf) | Planned | Encode and decode CloudEvents in each event format. |
| Protocol binding modules (HTTP, Kafka, AMQP, ...) | Planned | Transport-specific binary and structured content modes. |

---

## KMP Target Matrix

| Target | Platform | CI Runner | CI Status |
|--------|----------|-----------|-----------|
| `jvm` | Any (JDK 21+) | ubuntu | Tested |
| `js` (Node) | Node.js >= 18 | ubuntu | Tested |
| `wasmJs` (Node) | Node.js >= 18 | ubuntu | Tested |
| `linuxX64` | Linux x86-64 | ubuntu | Tested |
| `linuxArm64` | Linux ARM64 | ubuntu | Compiled |
| `macosArm64` | macOS Apple Silicon | macos | Tested |
| `iosSimulatorArm64` | iOS Simulator (Apple Silicon) | macos | Tested |
| `macosX64` | macOS x86-64 | macos | Compiled |
| `iosX64` | iOS Simulator (x86-64) | macos | Compiled |
| `iosArm64` | iOS device (ARM64) | macos | Compiled |
| `mingwX64` | Windows x86-64 | windows | Tested |

**"Tested"** — the test suite runs across the ubuntu, macOS, and Windows jobs on every pull
request that changes source (doc-only changes are skipped by the build workflow's `paths-ignore`).
**"Compiled"** — compiled and linked in the same jobs, but its tests are not executed:
`linuxArm64` has no ARM runner, the `macosX64`/`iosX64` variants share the same source as
their tested Apple-Silicon counterparts, and `iosArm64` is a device-only target.

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

## Working with CloudEvents

All types live in the `io.cloudevents.kotlin.core` package.

### Constructing an event

The `cloudEvent` DSL is the idiomatic entry point; a chainable builder and an immutable
`copy` are also available.

```kotlin
import io.cloudevents.kotlin.core.*
import kotlin.time.Instant

// DSL
val event = cloudEvent(id = "A234-1234-1234", source = "/sensors/tn-1234567", type = "com.example.temperature") {
    subject = "temperature"
    time = Instant.parse("2024-06-15T14:30:00Z")
    dataContentType = "application/json"
}

// Chainable builder
val built = CloudEventBuilder("A234-1234-1234", "/sensors/tn-1234567", "com.example.temperature")
    .withSubject("temperature")
    .build()

// Immutable copy — derives a new event, leaving the original untouched
val reidentified = event.copy(id = "B456-5678-5678")
```

### Attribute type system

Attribute values are drawn from the CloudEvents type system; every type round-trips to and
from its canonical string form.

```kotlin
BooleanValue(true).canonicalString                       // "true"
IntegerValue(42).canonicalString                         // "42"
BinaryValue(byteArrayOf(1, 2, 3)).canonicalString        // Base64 (RFC 4648)
TimestampValue.fromCanonicalString("2024-06-15T14:30:00Z")
```

### Extension attributes

Extensions use the same type system as core attributes, and their names are validated
against the CloudEvents naming rules when set.

```kotlin
val event = cloudEvent("A234-1234-1234", "/sensors/tn-1234567", "com.example.temperature") {
    extension("traceid", "abc-123")
    extension("retrycount", 3)
}

val raw: CloudEventAttributeValue? = event.getExtension("traceid")
val typed: StringValue? = event.getExtensionAs("traceid")
```

### Validation

Validation runs against the rules of the event's own `SpecVersion`. Strict mode (the
default) throws on an invalid event; lenient mode collects every violation. Each version
is checked against its own attribute set, type system, and naming rules — for example under
v1.0 a `datacontentencoding` attribute is a violation (it was removed in v1.0) and `dataschema`
must be an absolute URI, while under v0.3 `datacontentencoding` is valid and the schema URI is
`schemaurl`, which must be a URI-reference.

```kotlin
// Strict (default): throws CloudEventValidationException if invalid
event.validate()

// Lenient: inspect all violations without throwing
val result = event.validate(ValidationMode.LENIENT)
if (!result.isValid) {
    result.violations.forEach { println("${it.attribute}: ${it.message}") }
}
```

---

## JSON Event Format

The `:cloudevents-kotlin-json` module implements the CloudEvents
[JSON event format](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/formats/json-format.md)
for both supported wire versions (v1.0 and v0.3), on every KMP target. It encodes and
decodes structured-mode `application/cloudevents+json` documents and batch-mode
`application/cloudevents-batch+json` arrays. All types live in the
`io.cloudevents.kotlin.json` package and the entry point is the `JsonEventFormat` object.

```kotlin
import io.cloudevents.kotlin.json.JsonEventFormat
import io.cloudevents.kotlin.core.SpecVersion
import kotlin.time.Instant

// Encode a single event (structured mode).
val json: String = JsonEventFormat.encodeToString(event)

// Decode a structured-mode document.
val decoded: CloudEvent = JsonEventFormat.decodeFromString(json)

// Batch mode: a list of events ↔ a JSON array (mixed versions allowed).
val batch: String = JsonEventFormat.encodeBatch(listOf(event))
val events: List<CloudEvent> = JsonEventFormat.decodeBatch(batch)
```

The codec follows the spec's §3.1.1 data routing: a JSON-declared `datacontenttype` embeds
the payload as a raw JSON value under `data`; binary payloads route to `data_base64` (v1.0)
or base64-in-`data` with `datacontentencoding: "base64"` (v0.3); explicit `data: null` is
kept distinct from an absent `data` member. Timestamps are canonicalized to UTC (compare by
`Instant`, never by canonical-string bytes). Decoding validates strictly by default — an
invalid event throws `JsonEventFormatException` rather than being silently trusted.

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
