// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import io.cloudevents.kotlin.core.BooleanValue
import io.cloudevents.kotlin.core.CloudEventAttributeValue
import io.cloudevents.kotlin.core.CloudEventBuilder
import io.cloudevents.kotlin.core.IntegerValue
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.StringValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/** Collects unknown top-level members (satisfying the naming rules) as an open extension key set. */
internal fun readExtensions(root: JsonObject, specVersion: SpecVersion, builder: CloudEventBuilder) {
    for ((name, value) in root) {
        if (!isReservedMember(name, specVersion) && value !is JsonNull) {
            builder.extension(name, decodeExtensionValue(value, name))
        }
    }
}

internal fun isReservedMember(name: String, specVersion: SpecVersion): Boolean {
    if (name in CORE_RESERVED_MEMBERS) return true
    return when (specVersion) {
        SpecVersion.V1_0 -> name == "dataschema"
        SpecVersion.V0_3 -> name == "schemaurl" || name == "datacontentencoding"
    }
}

private val CORE_RESERVED_MEMBERS =
    setOf(
        "specversion",
        "id",
        "source",
        "type",
        "datacontenttype",
        "subject",
        "time",
        "data",
        "data_base64",
    )

private fun decodeExtensionValue(element: JsonElement, name: String): CloudEventAttributeValue {
    val value = toAttributeValue(element)
    return value ?: throw JsonEventFormatException(
        "Extension attribute '$name' is not a JSON primitive the CloudEvents type system can represent",
    )
}

private fun toAttributeValue(element: JsonElement): CloudEventAttributeValue? {
    val primitive = element as? JsonPrimitive ?: return null
    val integer = primitive.intOrNull
    val flag = primitive.booleanOrNull
    val text = primitive.contentOrNull
    return when {
        integer != null -> IntegerValue(integer)
        flag != null -> BooleanValue(flag)
        text != null -> StringValue(text)
        else -> null
    }
}
