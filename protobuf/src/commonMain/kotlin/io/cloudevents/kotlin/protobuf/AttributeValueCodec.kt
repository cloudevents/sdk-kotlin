// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

import io.cloudevents.kotlin.core.BinaryValue
import io.cloudevents.kotlin.core.BooleanValue
import io.cloudevents.kotlin.core.CloudEventAttributeValue
import io.cloudevents.kotlin.core.Formats
import io.cloudevents.kotlin.core.IntegerValue
import io.cloudevents.kotlin.core.StringValue
import io.cloudevents.kotlin.core.TimestampValue
import io.cloudevents.kotlin.core.UriReferenceValue
import io.cloudevents.kotlin.core.UriValue
import kotlin.time.Instant

/**
 * The `CloudEventAttributeValue` oneof codec: maps between the core type-system values and the
 * protobuf attribute-value wire message, shared by the encoder and decoder. The oneof has no
 * wire-level discriminator — exactly one branch must be set — so decode rejects ambiguous values
 * and treats an all-unset branch as "attribute omitted" (the Java SDK's `ATTR_NOT_SET`).
 */
@Suppress("TooManyFunctions") // Cohesive oneof codec: one small per-type encode/decode helper.
internal object AttributeValueCodec {
    /** Maps a CloudEvents type-system value to its `CloudEventAttributeValue` oneof branch. */
    fun encode(value: CloudEventAttributeValue): ProtoAttributeValue = when (value) {
        is BooleanValue -> ProtoAttributeValue(ceBoolean = value.value)
        is IntegerValue -> ProtoAttributeValue(ceInteger = value.value)
        is StringValue -> ProtoAttributeValue(ceString = value.value)
        is UriValue -> ProtoAttributeValue(ceUri = value.value)
        is UriReferenceValue -> ProtoAttributeValue(ceUriRef = value.value)
        is TimestampValue -> ProtoAttributeValue(ceTimestamp = toProtoTimestamp(value.value))
        is BinaryValue -> ProtoAttributeValue(ceBytes = value.value)
    }

    fun string(text: String): ProtoAttributeValue = ProtoAttributeValue(ceString = text)

    fun uri(text: String): ProtoAttributeValue = ProtoAttributeValue(ceUri = text)

    fun uriReference(text: String): ProtoAttributeValue = ProtoAttributeValue(ceUriRef = text)

    fun timestamp(instant: Instant): ProtoAttributeValue = ProtoAttributeValue(ceTimestamp = toProtoTimestamp(instant))

    private fun toProtoTimestamp(instant: Instant): ProtoTimestamp {
        if (instant.epochSeconds !in TIMESTAMP_SECONDS_MIN..TIMESTAMP_SECONDS_MAX) {
            throw ProtobufEventFormatException(
                "Attribute timestamp 'seconds' out of range: ${instant.epochSeconds}",
            )
        }
        return ProtoTimestamp(seconds = instant.epochSeconds, nanos = instant.nanosecondsOfSecond)
    }

    /**
     * Decodes a wire attribute value into a type-system value, honoring the proto3 oneof
     * last-member-wins selection from the wire scan: only the branch [selectedAttr] (the last
     * `attr` field seen) is converted. Returns `null` when no branch was present (an unset
     * oneof — the wire analog of the Java SDK's `ATTR_NOT_SET`, treated as omitted).
     */
    fun decode(wire: ProtoAttributeValue, name: String, selectedAttr: Int?): CloudEventAttributeValue? =
        when (selectedAttr) {
            WireFields.CE_BOOLEAN -> wire.ceBoolean?.let(::BooleanValue)
            WireFields.CE_INTEGER -> wire.ceInteger?.let(::IntegerValue)
            WireFields.CE_STRING -> wire.ceString?.let { validatedString(it, name) }
            WireFields.CE_BYTES -> wire.ceBytes?.let(::BinaryValue)
            WireFields.CE_URI -> wire.ceUri?.let(::UriValue)
            WireFields.CE_URI_REF -> wire.ceUriRef?.let(::UriReferenceValue)
            WireFields.CE_TIMESTAMP -> wire.ceTimestamp?.let { TimestampValue(toInstant(it)) }
            else -> null
        }

    /** Routes a wire string through core's String-checker: a violating value cannot form a valid attribute. */
    private fun validatedString(text: String, name: String): StringValue {
        val violation = Formats.firstStringViolation(text)
        if (violation != null) {
            throw ProtobufEventFormatException(
                "Attribute '$name' $violation and cannot form a CloudEvents String value",
            )
        }
        return StringValue(text)
    }

    fun asContextString(value: CloudEventAttributeValue, name: String): String = (value as? StringValue)?.value
        ?: throw ProtobufEventFormatException("Attribute '$name' must be a String value (ce_string)")

    fun asTimestamp(value: CloudEventAttributeValue, name: String): Instant = (value as? TimestampValue)?.value
        ?: throw ProtobufEventFormatException("Attribute '$name' must be a Timestamp value (ce_timestamp)")

    fun asUriLike(value: CloudEventAttributeValue, name: String): String = when (value) {
        is StringValue -> value.value
        is UriValue -> value.value
        is UriReferenceValue -> value.value
        else -> throw ProtobufEventFormatException(
            "Attribute '$name' must be a URI, URI-reference, or String value " +
                "(ce_uri, ce_uri_ref, or ce_string)",
        )
    }

    // Google's documented well-formed Timestamp range: the RFC 3339 years 0000-9999.
    private const val TIMESTAMP_SECONDS_MIN = -62_135_596_800L
    private const val TIMESTAMP_SECONDS_MAX = 253_402_300_799L
    private const val TIMESTAMP_NANOS_MAX = 999_999_999

    private fun toInstant(timestamp: ProtoTimestamp): Instant {
        if (timestamp.nanos !in 0..TIMESTAMP_NANOS_MAX) {
            throw ProtobufEventFormatException("Attribute timestamp 'nanos' out of range: ${timestamp.nanos}")
        }
        if (timestamp.seconds !in TIMESTAMP_SECONDS_MIN..TIMESTAMP_SECONDS_MAX) {
            throw ProtobufEventFormatException("Attribute timestamp 'seconds' out of range: ${timestamp.seconds}")
        }
        return Instant.fromEpochSeconds(timestamp.seconds, timestamp.nanos.toLong())
    }
}
