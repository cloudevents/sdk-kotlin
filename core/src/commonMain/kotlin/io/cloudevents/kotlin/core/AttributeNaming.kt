// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

/**
 * Enforcement of the CloudEvents attribute naming convention at construction time.
 *
 * Only the MUST-severity rules are enforced here (they reject at construction); the SHOULD-level
 * recommendations — a name length of at most 20 characters and a leading letter — are intentionally
 * not blocked and are left to be surfaced by validation. The same rules apply to core and extension
 * attribute names.
 */
internal object AttributeNaming {
    /** The payload is not a context attribute, so its name is reserved. */
    private const val RESERVED_DATA = "data"

    /** MUST rule: names consist only of lowercase ASCII letters and digits. */
    private val MUST_PATTERN = Regex("[a-z0-9]+")

    /**
     * Returns [name] unchanged if it satisfies the MUST-severity naming rules.
     *
     * @throws IllegalArgumentException if the name is empty, is the reserved name `data`, or
     *   contains any character outside `a-z` and `0-9`.
     */
    fun requireValidName(name: String): String {
        require(name.isNotEmpty()) { "Attribute name must not be empty" }
        require(name != RESERVED_DATA) {
            "'$RESERVED_DATA' is reserved for the event payload and must not be used as an attribute name"
        }
        require(MUST_PATTERN.matches(name)) {
            "Attribute name '$name' must consist only of lowercase letters (a-z) and digits (0-9)"
        }
        return name
    }
}
