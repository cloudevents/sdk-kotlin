// SPDX-License-Identifier: Apache-2.0

@file:Suppress("TooManyFunctions")

package io.cloudevents.kotlin.protobuf

import io.cloudevents.kotlin.core.CloudEvent
import io.cloudevents.kotlin.core.CloudEventAttributeValue
import io.cloudevents.kotlin.core.CloudEventBuilder
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.ValidationMode
import io.cloudevents.kotlin.core.validate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray

/**
 * The protobuf attribute and data readers: decode a `ProtoCloudEvent` wire message into a
 * [CloudEvent]. Like the JSON decoder, the pipeline is version-bootstrapped from `spec_version`,
 * validates strictly by default, and preserves data origin via the module's carriers.
 */
internal fun decodeEvent(bytes: ByteArray): CloudEvent {
    val wire = readWire(bytes)
    return buildEvent(wire, WireScan.scanEvent(bytes))
}

internal fun decodeBatchEvents(bytes: ByteArray): List<CloudEvent> {
    val batch = readBatch(bytes)
    val scans = WireScan.splitBatch(bytes).map { WireScan.scanEvent(it) }
    if (scans.size != batch.events.size) {
        throw ProtobufEventFormatException("Cannot align batch elements with the wire document")
    }
    return batch.events.zip(scans) { wire, scan -> buildEvent(wire, scan) }
}

@Suppress("TooGenericExceptionCaught") // kotlinx protobuf raises raw IndexOutOfBoundsException on truncated fields.
private fun readBatch(bytes: ByteArray): ProtoBatch = try {
    proto.decodeFromByteArray<ProtoBatch>(bytes)
} catch (e: SerializationException) {
    throw ProtobufEventFormatException("Cannot parse document as a CloudEvents protobuf batch", e)
} catch (e: IndexOutOfBoundsException) {
    throw ProtobufEventFormatException(
        "Cannot parse document as a CloudEvents protobuf batch (truncated or malformed)",
        e,
    )
}

@Suppress("TooGenericExceptionCaught") // kotlinx protobuf raises raw IndexOutOfBoundsException on truncated fields.
private fun readWire(bytes: ByteArray): ProtoCloudEvent = try {
    proto.decodeFromByteArray<ProtoCloudEvent>(bytes)
} catch (e: SerializationException) {
    throw ProtobufEventFormatException("Cannot parse document as protobuf", e)
} catch (e: IndexOutOfBoundsException) {
    throw ProtobufEventFormatException("Cannot parse document as protobuf (truncated or malformed message)", e)
}

private fun buildEvent(wire: ProtoCloudEvent, scan: WireScan.EventScan): CloudEvent {
    val specVersion = readSpecVersion(wire.specVersion)
    val builder = CloudEventBuilder(
        required(wire.id, "id"),
        required(wire.source, "source"),
        required(wire.type, "type"),
    ).withSpecVersion(specVersion)

    for ((name, value) in wire.attributes) {
        applyAttribute(builder, name, value, specVersion, scan.lastAttrField[name])
    }
    applyWireData(builder, wire, scan.lastDataField)

    val event = try {
        builder.build()
    } catch (e: IllegalArgumentException) {
        throw ProtobufEventFormatException("Document does not form a structurally well-formed CloudEvent", e)
    }
    // Strict by default: an invalid event (violating its own version's rules) throws here rather
    // than being silently trusted by the caller.
    try {
        event.validate(ValidationMode.STRICT)
    } catch (e: IllegalArgumentException) {
        throw ProtobufEventFormatException("Document is not a valid CloudEvents protobuf event", e)
    }
    return event
}

/** Proto3 has no presence for strings, so an absent/empty required attribute reads back as `""`. */
private fun required(value: String, name: String): String = if (value.isEmpty()) {
    throw ProtobufEventFormatException("Missing required '$name' attribute (absent or empty)")
} else {
    value
}

private fun readSpecVersion(wireValue: String): SpecVersion = try {
    SpecVersion.ofWireValue(wireValue)
} catch (e: IllegalArgumentException) {
    throw ProtobufEventFormatException("Unsupported 'specversion': $wireValue", e)
}

private fun applyAttribute(
    builder: CloudEventBuilder,
    name: String,
    wire: ProtoAttributeValue,
    specVersion: SpecVersion,
    selectedAttr: Int?,
) {
    if (name in RESERVED_MAP_NAMES) {
        throw ProtobufEventFormatException("The attributes map must not contain reserved member '$name'")
    }
    // The wire scan (proto3 oneof last-member-wins) picks which branch of an ambiguous attr value
    // to honor; an all-unset value (ATTR_NOT_SET) is omitted.
    val value = AttributeValueCodec.decode(wire, name, selectedAttr) ?: return
    if (name in specVersion.allAttributes) {
        applyContextAttribute(builder, name, value, specVersion)
    } else {
        addExtension(builder, name, value)
    }
}

private fun applyContextAttribute(
    builder: CloudEventBuilder,
    name: String,
    value: CloudEventAttributeValue,
    specVersion: SpecVersion,
) {
    when (name) {
        "datacontenttype" -> builder.dataContentType = AttributeValueCodec.asContextString(value, name)
        "subject" -> builder.subject = AttributeValueCodec.asContextString(value, name)
        "time" -> builder.time = AttributeValueCodec.asTimestamp(value, name)
        "datacontentencoding" -> builder.dataContentEncoding = AttributeValueCodec.asContextString(value, name)
        "dataschema" -> builder.dataSchema = AttributeValueCodec.asUriLike(value, name)
        "schemaurl" -> builder.dataSchema = AttributeValueCodec.asUriLike(value, name)
        else -> error("Unexpected context attribute '$name' for ${specVersion.wireValue}")
    }
}

private fun addExtension(builder: CloudEventBuilder, name: String, value: CloudEventAttributeValue) {
    try {
        builder.extension(name, value)
    } catch (e: IllegalArgumentException) {
        throw ProtobufEventFormatException("Attribute '$name' is not a valid extension attribute", e)
    }
}

private val RESERVED_MAP_NAMES = setOf("id", "source", "specversion", "type")

private fun applyWireData(builder: CloudEventBuilder, wire: ProtoCloudEvent, lastDataField: Int?) {
    // The wire scan (proto3 oneof last-member-wins) selects which of the decoded nullable branches
    // is the message's data; kotlinx decodes every present branch, so without the scan the model
    // cannot tell which came last on the wire.
    builder.data = when (lastDataField) {
        WireFields.BINARY_DATA -> wire.binaryData?.let { ProtobufEventData.Binary(it) }
        WireFields.TEXT_DATA -> wire.textData?.let { ProtobufEventData.Text(it) }
        WireFields.PROTO_DATA -> wire.protoData?.let { ProtobufEventData.Proto(it.typeUrl, it.value) }
        else -> null
    }
}
