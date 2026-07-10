# Security Policy

## Reporting a Vulnerability

The CloudEvents Kotlin SDK follows the CloudEvents project and CNCF vulnerability
disclosure process. If you discover a security vulnerability in this SDK, **do not
open a public GitHub issue**.

Instead, please follow the process described in the CloudEvents security policy:

**https://github.com/cloudevents/.github/blob/main/SECURITY.md**

If that resource is unavailable, refer to the CNCF vulnerability disclosure process:

**https://github.com/cncf/foundation/blob/main/security-process.md**

Reports are reviewed by the CloudEvents maintainers and handled according to the
severity and impact of the finding.

## Attack Surface of This Library

The CloudEvents Kotlin SDK is a **pure data-model library** with no runtime server
component, no credential handling, and no network communication of its own. It does
not open ports, manage sessions, or store user data.

The primary security-relevant operations in this library are:

- **Parsing and validating CloudEvent context attributes** — malformed events
  (oversized inputs, unexpected types, crafted attribute values) are the most
  likely attack vector. The SDK performs type checking and format validation on
  all context attributes and will reject events that do not conform to the
  CloudEvents specification.
- **Type confusion** — the CloudEvents type system (Boolean, Integer, String, Binary,
  URI, URI-reference, Timestamp) must be enforced strictly. The SDK rejects values
  that do not satisfy the declared type constraints.

## Sensitive Data Guidance

CloudEvents context attributes (id, source, type, subject, etc.) are **not designed
to carry sensitive or confidential information**. The CloudEvents specification
treats context attributes as non-confidential metadata.

If you need to carry sensitive data in a CloudEvent, place it in the **opaque `data`
field** and protect it at the transport layer (TLS, encryption at rest, access control).
Do not embed secrets, credentials, or personally identifiable information in context
attribute values.

## Scope

Security reports are welcome for:

- Validation bypass: a crafted event that passes validation but violates the spec
- Type confusion: a value that is accepted as the wrong CloudEvents type
- Denial of service via malformed input: inputs that cause the library to hang,
  crash, or consume excessive resources during parsing or validation

Out of scope:

- Vulnerabilities in dependencies that are not triggered by this library's usage
- Issues in downstream protocol bindings or transport layers (those are separate
  from this core SDK)
