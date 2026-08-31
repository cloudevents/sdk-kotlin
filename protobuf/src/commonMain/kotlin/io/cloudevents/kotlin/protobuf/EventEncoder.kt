// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

import io.cloudevents.kotlin.core.CloudEvent
import io.cloudevents.kotlin.core.CloudEventAttributeValue
import io.cloudevents.kotlin.core.CloudEventData
import io.cloudevents.kotlin.core.SpecVersion
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

/** The media type whose payload is a `google.protobuf.Any` (a `proto_data`-shaped payload). */
internal const val PROTO_DATA_CONTENT_TYPE = "application/protobuf"

/** The batch media type for a `CloudEventBatch` envelope. */
internal const val BATCH_MEDIA_TYPE = "application/cloudevents-batch+protobuf"

/** Encodes [event] into a structured-mode `application/cloudevents+protobuf` message. */
internal fun encodeEvent(event: CloudEvent): ByteArray = proto.encodeToByteArray(toWire(event))

/** Encodes a list of events into a `CloudEventBatch` message. */
internal fun encodeBatchEvents(events: List<CloudEvent>): ByteArray =
    proto.encodeToByteArray(ProtoBatch(events.map(::toWire)))

/** The three `oneof data` branches, at most one non-null. */
private data class WireData(val binary: ByteArray? = null, val text: String? = null, val proto: ProtoAny? = null)

internal fun toWire(event: CloudEvent): ProtoCloudEvent {
    val attributes = LinkedHashMap<String, ProtoAttributeValue>()
    putContextAttributes(attributes, event)
    putExtensions(attributes, event.extensions)
    val data = toWireData(event)
    return ProtoCloudEvent(
        id = event.id,
        source = event.source,
        specVersion = event.specVersion.wireValue,
        type = event.type,
        attributes = attributes,
        binaryData = data.binary,
        textData = data.text,
        protoData = data.proto,
    )
}

private fun putContextAttributes(attributes: MutableMap<String, ProtoAttributeValue>, event: CloudEvent) {
    event.dataContentType?.let { attributes["datacontenttype"] = AttributeValueCodec.string(it) }
    event.subject?.let { attributes["subject"] = AttributeValueCodec.string(it) }
    event.time?.let { attributes["time"] = AttributeValueCodec.timestamp(it) }
    when (event.specVersion) {
        SpecVersion.V1_0 -> event.dataSchema?.let { attributes["dataschema"] = AttributeValueCodec.uri(it) }
        SpecVersion.V0_3 -> {
            event.dataSchema?.let { attributes["schemaurl"] = AttributeValueCodec.uriReference(it) }
            event.dataContentEncoding?.let { attributes["datacontentencoding"] = AttributeValueCodec.string(it) }
        }
    }
}

private fun putExtensions(
    attributes: MutableMap<String, ProtoAttributeValue>,
    extensions: Map<String, CloudEventAttributeValue>,
) {
    for ((name, value) in extensions) {
        attributes[name] = AttributeValueCodec.encode(value)
    }
}

private fun toWireData(event: CloudEvent): WireData {
    val data = event.data ?: return WireData()
    return when (data) {
        is ProtobufEventData.Proto -> WireData(proto = ProtoAny(data.typeUrl, data.value))
        is ProtobufEventData.Text -> WireData(text = data.text)
        is ProtobufEventData.Binary -> WireData(binary = data.bytes)
        else -> routePlainData(event, data)
    }
}

/**
 * Routes a plain, non-carrier payload per the reference sdk-java serializer: a
 * `application/protobuf` declared payload must BE a `google.protobuf.Any`; a
 * text-declared content type rides in `text_data`; everything else is binary (`binary_data`).
 */
private fun routePlainData(event: CloudEvent, data: CloudEventData): WireData {
    val contentType = event.dataContentType
    if (contentType == null) return WireData(binary = data.toBytes())
    return when {
        contentType.substringBefore(';').trim().equals(PROTO_DATA_CONTENT_TYPE, ignoreCase = true) ->
            WireData(proto = parseAnyData(data.toBytes(), contentType))
        isTextContent(contentType) -> WireData(text = decodeTextData(data.toBytes(), contentType))
        else -> WireData(binary = data.toBytes())
    }
}

/** Parses a payload declared `application/protobuf` into an `Any`, failing loudly when invalid. */
@Suppress("TooGenericExceptionCaught") // kotlinx protobuf raises raw IndexOutOfBoundsException on truncated fields.
private fun parseAnyData(bytes: ByteArray, contentType: String): ProtoAny = try {
    proto.decodeFromByteArray<ProtoAny>(bytes)
} catch (e: SerializationException) {
    throw ProtobufEventFormatException(
        "Cannot serialize event data as protobuf: declared datacontenttype=$contentType " +
            "but the payload is not a valid google.protobuf.Any",
        e,
    )
} catch (e: IndexOutOfBoundsException) {
    throw ProtobufEventFormatException(
        "Cannot serialize event data as protobuf: declared datacontenttype=$contentType " +
            "but the payload is not a valid google.protobuf.Any (truncated or malformed)",
        e,
    )
}

/** Decodes a text-declared payload as UTF-8, failing loudly when the bytes are not valid UTF-8. */
private fun decodeTextData(bytes: ByteArray, contentType: String): String = try {
    bytes.decodeToString(throwOnInvalidSequence = true)
} catch (e: kotlin.text.CharacterCodingException) {
    throw ProtobufEventFormatException(
        "Cannot serialize event data as text: declared datacontenttype=$contentType " +
            "but the payload is not valid UTF-8",
        e,
    )
}

/**
 * True when [contentType] declares textually-representable data per the reference sdk-java
 * `ProtoSupport.isTextContent` — a `text/`-prefixed type, `application/json`, `application/xml`,
 * or a `+json`/`+xml` suffix. Divergence from the reference: media-type parameters are stripped and
 * matching is case-insensitive, so `text/plain; charset=utf-8` is text (the Java SDK's raw
 * `startsWith` would misroute non-`text/` parameterized types).
 */
internal fun isTextContent(contentType: String?): Boolean {
    if (contentType == null) return false
    val mediaType = contentType.substringBefore(';').trim().lowercase()
    return mediaType.startsWith("text/") ||
        mediaType == "application/json" ||
        mediaType == "application/xml" ||
        mediaType.endsWith("+json") ||
        mediaType.endsWith("+xml")
}
