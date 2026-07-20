// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

/**
 * The CloudEvents specification version an event conforms to, carried in the `specversion`
 * context attribute.
 *
 * A single core artifact interprets both supported wire versions at runtime; each constant owns
 * its own version contract (attribute set, type system, and naming rules).
 */
enum class SpecVersion(
    val wireValue: String,
    val mandatoryAttributes: Set<String>,
    val optionalAttributes: Set<String>,
) {
    /**
     * CloudEvents v1.0. The `specversion` wire value is `1.0`: the spec omits
     * patch revisions from this attribute so serialized events stay stable across specification
     * patch releases.
     */
    V1_0(
        "1.0",
        setOf("id", "source", "specversion", "type"),
        setOf("datacontenttype", "dataschema", "subject", "time"),
    ),

    /**
     * CloudEvents v0.3. Differs from v1.0 in its context attributes: `schemaurl` (a URI-reference)
     * in place of `dataschema`, plus the `datacontentencoding` attribute that v1.0 removed.
     */
    V0_3(
        "0.3",
        setOf("id", "source", "specversion", "type"),
        setOf("datacontenttype", "datacontentencoding", "schemaurl", "subject", "time"),
    ),
    ;

    /** Every context-attribute name defined by this version (mandatory and optional). */
    val allAttributes: Set<String> get() = mandatoryAttributes + optionalAttributes

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
