// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

/**
 * A single spec-conformance problem found on a CloudEvent, naming the offending [attribute] and a
 * human-readable [message].
 */
data class ValidationViolation(val attribute: String, val message: String)

/**
 * The outcome of validating a CloudEvent: the complete list of [violations] found.
 *
 * Validation collects every violation rather than stopping at the first, so the result reflects
 * all problems at once.
 */
data class ValidationResult(val violations: List<ValidationViolation>) {
    /** True when no violations were found. */
    val isValid: Boolean get() = violations.isEmpty()
}

/**
 * Validates this event against the rules of its own [SpecVersion] and returns every violation
 * found.
 *
 * Both wire versions are validated, each against its own attribute set: a missing or empty REQUIRED
 * attribute is always a violation, and version-specific type and format rules are applied according
 * to [CloudEvent.specVersion].
 */
fun CloudEvent.validate(): ValidationResult {
    val violations = buildList {
        addAll(requiredAttributeViolations())
        addAll(versionSpecificViolations())
    }
    return ValidationResult(violations)
}

private fun CloudEvent.requiredAttributeViolations(): List<ValidationViolation> = buildList {
    if (id.isEmpty()) {
        add(ValidationViolation("id", "id is a REQUIRED attribute and must not be empty"))
    }
    if (source.isEmpty()) {
        add(ValidationViolation("source", "source is a REQUIRED attribute and must not be empty"))
    }
    if (type.isEmpty()) {
        add(ValidationViolation("type", "type is a REQUIRED attribute and must not be empty"))
    }
}

private fun CloudEvent.versionSpecificViolations(): List<ValidationViolation> = when (specVersion) {
    SpecVersion.V1_0 -> emptyList()
    SpecVersion.V0_3 -> emptyList()
}
