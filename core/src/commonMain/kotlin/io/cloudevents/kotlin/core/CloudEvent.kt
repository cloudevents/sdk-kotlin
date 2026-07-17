// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

import kotlin.time.Instant

/**
 * The canonical, transport- and format-independent representation of an event: its context
 * attributes plus optional [data].
 *
 * A `CloudEvent` is immutable and always structurally well-formed — every [SpecVersion.V1_0]
 * REQUIRED attribute is present. Structural well-formedness is not the same as spec conformance:
 * semantic checks (non-empty values, RFC 3339 timestamps, URI grammar) are a separate validation
 * step and are not enforced by construction.
 *
 * Instances are created through the construction API rather than this constructor directly.
 */
// The parameter count mirrors the spec-defined context-attribute set, not incidental
// complexity; the constructor is internal and callers use the construction API instead.
@Suppress("LongParameterList")
class CloudEvent internal constructor(
    val id: String,
    val source: String,
    val type: String,
    val specVersion: SpecVersion = SpecVersion.V1_0,
    val dataContentType: String? = null,
    val dataSchema: String? = null,
    val subject: String? = null,
    val time: Instant? = null,
    val data: CloudEventData? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CloudEvent) return false
        return id == other.id &&
            source == other.source &&
            type == other.type &&
            specVersion == other.specVersion &&
            dataContentType == other.dataContentType &&
            dataSchema == other.dataSchema &&
            subject == other.subject &&
            time == other.time &&
            data == other.data
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + specVersion.hashCode()
        result = 31 * result + dataContentType.hashCode()
        result = 31 * result + dataSchema.hashCode()
        result = 31 * result + subject.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }

    override fun toString(): String = "CloudEvent(" +
        "specversion=${specVersion.wireValue}, id=$id, source=$source, type=$type, " +
        "datacontenttype=$dataContentType, dataschema=$dataSchema, subject=$subject, time=$time, " +
        "data=$data" +
        ")"
}
