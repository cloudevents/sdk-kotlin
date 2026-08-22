// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.TimestampValue
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun readSpecVersion(root: JsonObject): SpecVersion {
    val value = requiredString(root, "specversion")
    return try {
        SpecVersion.ofWireValue(value)
    } catch (e: IllegalArgumentException) {
        throw JsonEventFormatException("Unsupported 'specversion': $value", e)
    }
}

internal fun requiredString(root: JsonObject, name: String): String =
    readStringMember(root, name) ?: throw JsonEventFormatException("Missing required '$name' attribute")

internal fun optionalString(root: JsonObject, name: String): String? = readStringMember(root, name)

/** Reads a member that must be a JSON string when present; `null`/absent/null-literal yield null. */
internal fun readStringMember(root: JsonObject, name: String): String? {
    val element = root[name]
    val text = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
    return when {
        element == null -> null
        element is JsonNull -> null
        text == null -> throw JsonEventFormatException("Attribute '$name' must be a JSON string")
        else -> text
    }
}

internal fun readTimestamp(text: String): kotlin.time.Instant = try {
    TimestampValue.fromCanonicalString(text).value
} catch (e: IllegalArgumentException) {
    throw JsonEventFormatException("'time' is not a valid RFC 3339 date-time: $text", e)
}
