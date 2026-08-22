// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import io.cloudevents.kotlin.core.CloudEvent
import io.cloudevents.kotlin.core.CloudEventBuilder
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.ValidationMode
import io.cloudevents.kotlin.core.validate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** A lenient parse so the decoder can apply its own structural and semantic rules uniformly. */
private val json = Json { ignoreUnknownKeys = true }

/**
 * Decodes a structured-mode `application/cloudevents+json` document into a [CloudEvent].
 *
 * Version-bootstraps from `specversion`, preserves extension members as an open key set, routes
 * `data`/`data_base64` to the module's data-origin carrier per version and §3.1.1, and validates the
 * resulting event strictly ([ValidationMode.STRICT]) so an invalid document throws rather than being
 * silently trusted.
 *
 * @throws JsonEventFormatException if [text] is not a valid CloudEvents JSON document (wrong shape,
 *   unknown `specversion`, or a semantic validation violation, wrapping the core exception).
 */
internal fun decodeEvent(text: String): CloudEvent {
    val root = parseObject(text)
    return buildEventFromObject(root)
}

internal fun decodeBatchEvents(text: String): List<CloudEvent> {
    val root = parse(text)
    val array = root as? JsonArray ?: throw JsonEventFormatException(
        "A batch-mode CloudEvents document must be a JSON array",
    )
    return array.map { optObject(it) }
}

private fun optObject(element: JsonElement): CloudEvent = if (element is JsonObject) {
    buildEventFromObject(element)
} else {
    throw JsonEventFormatException(
        "Each batch element must be a CloudEvents JSON object",
    )
}

private fun parseObject(text: String): JsonObject {
    val root = parse(text)
    if (root !is JsonObject) {
        throw JsonEventFormatException("A structured-mode CloudEvents document must be a JSON object")
    }
    return root
}

private fun parse(text: String): JsonElement = try {
    json.parseToJsonElement(text)
} catch (e: IllegalArgumentException) {
    throw JsonEventFormatException("Cannot parse document as JSON", e)
}

private fun buildEventFromObject(root: JsonObject): CloudEvent {
    val specVersion = readSpecVersion(root)
    val builder = CloudEventBuilder(
        requiredString(root, "id"),
        requiredString(root, "source"),
        requiredString(root, "type"),
    ).withSpecVersion(specVersion)

    builder.dataContentType = optionalString(root, "datacontenttype")
    builder.subject = optionalString(root, "subject")
    builder.time = optionalString(root, "time")?.let { readTimestamp(it) }
    configureVersionAttributes(builder, root, specVersion)
    builder.data = readData(root, specVersion, builder.dataContentType)
    readExtensions(root, specVersion, builder)

    val event = buildEvent(builder)
    validateEvent(event)
    return event
}

private fun configureVersionAttributes(builder: CloudEventBuilder, root: JsonObject, specVersion: SpecVersion) {
    when (specVersion) {
        SpecVersion.V1_0 -> builder.dataSchema = optionalString(root, "dataschema")
        SpecVersion.V0_3 -> {
            builder.dataSchema = optionalString(root, "schemaurl")
            builder.dataContentEncoding = optionalString(root, "datacontentencoding")
        }
    }
}

private fun buildEvent(builder: CloudEventBuilder): CloudEvent = try {
    builder.build()
} catch (e: IllegalArgumentException) {
    throw JsonEventFormatException("Document does not form a structurally well-formed CloudEvent", e)
}

private fun validateEvent(event: CloudEvent) {
    // Strict by default: an invalid event (violating its own version's rules) throws here rather than
    // being silently trusted by the caller.
    try {
        event.validate(ValidationMode.STRICT)
    } catch (e: IllegalArgumentException) {
        throw JsonEventFormatException("Document is not a valid CloudEvents JSON event", e)
    }
}
