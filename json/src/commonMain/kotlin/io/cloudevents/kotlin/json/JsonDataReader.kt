// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import io.cloudevents.kotlin.core.CloudEventData
import io.cloudevents.kotlin.core.SpecVersion
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlin.io.encoding.Base64

/**
 * Routes the data trio per ADR 0005's fixed member-priority order and §3.1.1, rejecting the
 * `data`+`data_base64` contradiction and producing the appropriate data-origin carrier.
 */
internal fun readData(root: JsonObject, specVersion: SpecVersion, contentType: String?): CloudEventData? {
    val hasData = root.containsKey("data")
    val isV1 = specVersion == SpecVersion.V1_0
    return when {
        contradicts(isV1, hasData, root) -> throw JsonEventFormatException(
            "An event cannot carry both 'data' and 'data_base64' (contradictory §3.1.1 payload)",
        )
        explicitNull(hasData, root) -> NullData
        isV1 -> readV1Data(root, hasData, contentType)
        else -> readV03Data(root, hasData, contentType)
    }
}

private fun contradicts(isV1: Boolean, hasData: Boolean, root: JsonObject): Boolean =
    isV1 && hasData && root.containsKey("data_base64")

private fun explicitNull(hasData: Boolean, root: JsonObject): Boolean = hasData && root["data"] is JsonNull

private fun readV1Data(root: JsonObject, hasData: Boolean, contentType: String?): CloudEventData? {
    val base64 = root["data_base64"]?.let { decodeBase64Member(it, "data_base64") }
    val v1 = if (hasData) routeDataElement(root.getValue("data"), contentType) else null
    return base64 ?: v1
}

private fun readV03Data(root: JsonObject, hasData: Boolean, contentType: String?): CloudEventData? {
    if (!hasData) return null // ABSENT.
    val element = root.getValue("data")
    return when {
        optionalString(root, "datacontentencoding") == "base64" -> decodeBase64Member(element, "data")
        else -> routeDataElement(element, contentType)
    }
}

private fun routeDataElement(element: JsonElement, contentType: String?): CloudEventData {
    val jsonDeclared = isJsonContentType(contentType)
    val text = (element as? JsonPrimitive)?.content
    return when {
        jsonDeclared -> JsonData(element)
        text != null -> StringData(text)
        else -> throw JsonEventFormatException(
            "With a non-JSON datacontenttype (or none), 'data' must be a JSON string; " +
                "got a ${jsonKind(element)} member.",
        )
    }
}

private fun decodeBase64Member(element: JsonElement, name: String): Base64Data {
    val text = (element as? JsonPrimitive)?.content
        ?: throw JsonEventFormatException("'$name' must be a JSON string holding Base64-encoded data")
    return try {
        Base64Data(Base64.decode(text))
    } catch (e: IllegalArgumentException) {
        throw JsonEventFormatException("'$name' is not valid Base64", e)
    }
}

internal fun jsonKind(element: JsonElement): String = when (element) {
    is JsonObject -> "JSON object"
    is kotlinx.serialization.json.JsonArray -> "JSON array"
    is JsonPrimitive ->
        when {
            element.booleanOrNull != null -> "JSON boolean"
            element.contentOrNull != null -> "JSON string"
            else -> "JSON number"
        }
    JsonNull -> "JSON null"
}
