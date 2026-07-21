# Changelog

All notable changes to this project are documented in this file.

## 0.1.0 - 2026-07-21

### Features

- **core:** Empty :core module with io.cloudevents coordinates and smoke test
- **core:** Model for CloudEvent and SpecVersion (#13)
- **core:** Attribute type system with canonical-string round-trip (#3)
- **core:** Opaque-bytes seam CloudEventData (#4)
- **core:** Message-SPI reader/writer interfaces (#5)
- **core:** Enforce attribute naming rules at construction (#6)
- **core:** Extension attribute support (#7)
- **core:** CloudEvent DSL, fluent builder, and immutable copy (#8)
- **core:** Per-event validate() branching on specversion (#9)
- **core:** Type and format validators for the attribute type system (#10)
- **core:** Strict and lenient validation modes (#11)
- **core:** Full v0.3 attribute model and version-aware conformance (#14)

### Documentation

- Contributing, code of conduct, security policy, and readme
- **readme:** Document features, support matrix, and target coverage

### Build & CI

- Kotlin multiplatform scaffold, convention plugins, and versioning
- Pr-checks, push build, release, and dependency-review workflows
- Test Apple and Windows targets on a native matrix
- Harden supply chain with dependency verification, SBOM, and OSV scanning
- **release:** Publish signed KMP artifact matrix to Maven Central
- **release:** Generate changelog and release notes with git-cliff

### Miscellaneous

- Harden renovate config for kotlin multiplatform
- Relax commit subject-case rule to allow proper nouns
