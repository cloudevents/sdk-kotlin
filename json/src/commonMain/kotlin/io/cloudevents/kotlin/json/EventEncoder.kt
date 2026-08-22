// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import io.cloudevents.kotlin.core.BinaryValue
import io.cloudevents.kotlin.core.BooleanValue
import io.cloudevents.kotlin.core.CloudEvent
import io.cloudevents.kotlin.core.CloudEventAttributeValue
import io.cloudevents.kotlin.core.CloudEventData
import io.cloudevents.kotlin.core.IntegerValue
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.StringValue
import io.cloudevents.kotlin.core.TimestampValue
import io.cloudevents.kotlin.core.UriReferenceValue
import io.cloudevents.kotlin.core.UriValue
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
private val json = Json { prettyPrint = false }

/** The batch media type for a JSON array of CloudEvents. */
internal const val BATCH_MEDIA_TYPE = "application/cloudevents-batch+json"

/** Encodes [event] into a structured-mode `application/cloudevents+json` document (RFC 8259). */
internal fun encodeEvent(event: CloudEvent): String = encodeToElement(event).toString()

/** Encodes a single event as a [JsonObject]. */
internal fun encodeToElement(event: CloudEvent): JsonObject {
    val members = LinkedHashMap<String, JsonElement>()
    putContextAttributes(members, event)
    putExtensions(members, event.extensions)
    putData(members, event)
    return JsonObject(members)
}

private fun putContextAttributes(members: MutableMap<String, JsonElement>, event: CloudEvent) {
    members["specversion"] = JsonPrimitive(event.specVersion.wireValue)
    members["id"] = JsonPrimitive(event.id)
    members["source"] = JsonPrimitive(event.source)
    members["type"] = JsonPrimitive(event.type)
    event.dataContentType?.let { members["datacontenttype"] = JsonPrimitive(it) }
    event.subject?.let { members["subject"] = JsonPrimitive(it) }
    event.time?.let { members["time"] = JsonPrimitive(it.toString()) }
    when (event.specVersion) {
        SpecVersion.V1_0 -> event.dataSchema?.let { members["dataschema"] = JsonPrimitive(it) }
        SpecVersion.V0_3 -> {
            event.dataSchema?.let { members["schemaurl"] = JsonPrimitive(it) }
            event.dataContentEncoding?.let { members["datacontentencoding"] = JsonPrimitive(it) }
        }
    }
}

private fun putExtensions(
    members: MutableMap<String, JsonElement>,
    extensions: Map<String, CloudEventAttributeValue>,
) {
    for ((name, value) in extensions) {
        members[name] = encodeAttributeValue(value)
    }
}

/** Maps a CloudEvents type-system value to its JSON representation per the JSON format type mapping. */
internal fun encodeAttributeValue(value: CloudEventAttributeValue): JsonElement = when (value) {
    is BooleanValue -> JsonPrimitive(value.value)
    is IntegerValue -> JsonPrimitive(value.value)
    is StringValue -> JsonPrimitive(value.value)
    is UriValue -> JsonPrimitive(value.value)
    is UriReferenceValue -> JsonPrimitive(value.value)
    is TimestampValue -> JsonPrimitive(value.value.toString())
    is BinaryValue -> JsonPrimitive(value.canonicalString)
}

private fun putData(members: MutableMap<String, JsonElement>, event: CloudEvent) {
    val data = event.data ?: return // ABSENT: no data member emitted.
    when (data) {
        is NullData -> members["data"] = JsonNull
        is JsonData -> members["data"] = data.element
        is StringData -> members["data"] = JsonPrimitive(data.text)
        is Base64Data -> putBase64Data(members, event, data.toBytes())
        else -> putPlainData(members, event, data)
    }
}

private fun putBase64Data(members: MutableMap<String, JsonElement>, event: CloudEvent, bytes: ByteArray) {
    val base64 = kotlin.io.encoding.Base64.encode(bytes)
    when (event.specVersion) {
        SpecVersion.V1_0 -> members["data_base64"] = JsonPrimitive(base64)
        SpecVersion.V0_3 -> {
            members["datacontentencoding"] = JsonPrimitive("base64")
            members["data"] = JsonPrimitive(base64)
        }
    }
}

/**
 * Routes a plain, non-carrier payload per §3.1.1 from its `datacontenttype` alone (no origin carrier
 * recorded). Mirrors the reference sdk-java serializer: JSON-declared content is embedded as a raw JSON
 * value; otherwise the opaque bytes are treated as binary and Base64-encoded.
 */
private fun putPlainData(members: MutableMap<String, JsonElement>, event: CloudEvent, data: CloudEventData) {
    val contentType = event.dataContentType
    if (isJsonContentType(contentType)) {
        members["data"] = parseJsonData(data.toBytes(), contentType)
    } else {
        putBase64Data(members, event, data.toBytes())
    }
}

/** Parses a non-null JSON-declared payload into a raw JSON value, failing loudly when it is invalid. */
private fun parseJsonData(bytes: ByteArray, contentType: String?): JsonElement {
    val text = bytes.decodeToString()
    return try {
        json.parseToJsonElement(text)
    } catch (e: SerializationException) {
        throw JsonEventFormatException(
            "Cannot serialize event data as JSON: declared datacontenttype=$contentType " +
                "but the payload is not a valid JSON document",
            e,
        )
    }
}

/** Encodes a list of events into a batch-mode `application/cloudevents-batch+json` JSON array. */
internal fun encodeBatchEvents(events: List<CloudEvent>): String {
    if (events.isEmpty()) return "[]"
    return buildJsonArray { for (event in events) add(encodeToElement(event)) }.toString()
}
