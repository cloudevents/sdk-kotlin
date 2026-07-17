// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

/**
 * The CloudEvents specification version an event conforms to, carried in the `specversion`
 * context attribute.
 *
 * A single core artifact interprets both supported wire versions at runtime; each constant owns
 * its own version contract (attribute set, type system, and naming rules).
 */
enum class SpecVersion(val wireValue: String) {
    /**
     * CloudEvents v1.0. The `specversion` wire value is `1.0`: the spec omits
     * patch revisions from this attribute so serialized events stay stable across specification
     * patch releases.
     */
    V1_0("1.0"),

    /** CloudEvents v0.3. */
    V0_3("0.3"),
    ;

    companion object {
        /**
         * Resolves the [SpecVersion] for a `specversion` wire value.
         *
         * @throws IllegalArgumentException if the value is not a supported CloudEvents version,
         *   so the SDK never holds an event whose version it cannot interpret.
         */
        fun ofWireValue(wireValue: String): SpecVersion = entries.firstOrNull { it.wireValue == wireValue }
            ?: throw IllegalArgumentException(
                "Unsupported CloudEvents spec version: '$wireValue' " +
                    "(supported: ${entries.joinToString { it.wireValue }})",
            )
    }
}
