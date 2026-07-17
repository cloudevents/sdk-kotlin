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
 * Validates this event against the rules of its own [SpecVersion], collecting every violation found.
 *
 * Both wire versions are validated, each against its own attribute set: a missing or empty REQUIRED
 * attribute is always a violation, and version-specific type and format rules are applied according
 * to [CloudEvent.specVersion].
 *
 * In [ValidationMode.STRICT] (the default) an invalid event raises [CloudEventValidationException];
 * in [ValidationMode.LENIENT] the collected [ValidationResult] is returned without throwing.
 */
fun CloudEvent.validate(mode: ValidationMode = ValidationMode.STRICT): ValidationResult {
    val result = ValidationResult(collectViolations())
    if (mode == ValidationMode.STRICT && !result.isValid) {
        throw CloudEventValidationException(result)
    }
    return result
}

private fun CloudEvent.collectViolations(): List<ValidationViolation> = buildList {
    addAll(requiredAttributeViolations())
    addAll(commonFormatViolations())
    addAll(versionSpecificViolations())
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

/** Format rules shared by every version: `source` grammar and the `String`-type character rules. */
private fun CloudEvent.commonFormatViolations(): List<ValidationViolation> = buildList {
    if (source.isNotEmpty() && !Formats.isUriReference(source)) {
        add(ValidationViolation("source", "source must be an RFC 3986 URI-reference"))
    }
    addStringViolation("id", id)
    addStringViolation("type", type)
    subject?.let {
        if (it.isEmpty()) {
            add(ValidationViolation("subject", "subject must be a non-empty string when present"))
        } else {
            addStringViolation("subject", it)
        }
    }
    dataContentType?.let { addStringViolation("datacontenttype", it) }
    for ((name, value) in extensions) {
        if (value is StringValue) addStringViolation(name, value.value)
    }
}

private fun MutableList<ValidationViolation>.addStringViolation(attribute: String, value: String) {
    Formats.firstStringViolation(value)?.let { add(ValidationViolation(attribute, "$attribute $it")) }
}

private fun CloudEvent.versionSpecificViolations(): List<ValidationViolation> = when (specVersion) {
    SpecVersion.V1_0 -> buildList {
        dataSchema?.let {
            if (!Formats.isAbsoluteUri(it)) {
                add(ValidationViolation("dataschema", "dataschema must be an absolute URI"))
            }
        }
    }

    SpecVersion.V0_3 -> emptyList()
}
