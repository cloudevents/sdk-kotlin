// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

/** How [validate] reacts to a CloudEvent that has one or more violations. */
enum class ValidationMode {
    /** Throw [CloudEventValidationException] if the event is invalid. This is the default. */
    STRICT,

    /** Never throw; return the collected [ValidationResult] enumerating every violation. */
    LENIENT,
}

/**
 * Thrown by [validate] in [ValidationMode.STRICT] when a CloudEvent is invalid. The full
 * [ValidationResult] — every violation found, not just the first — is available via [result].
 */
class CloudEventValidationException internal constructor(val result: ValidationResult) :
    IllegalArgumentException(
        "CloudEvent is invalid: " +
            result.violations.joinToString("; ") { "${it.attribute}: ${it.message}" },
    )
