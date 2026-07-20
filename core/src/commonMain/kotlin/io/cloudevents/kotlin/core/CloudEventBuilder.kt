// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

import kotlin.time.Instant

/**
 * Mutable builder for the immutable [CloudEvent].
 *
 * Attributes can be set either through the assignable properties — used by the [cloudEvent] DSL —
 * or through the chainable `with*` methods. Extension names are validated against the MUST naming
 * rules as they are set. [build] enforces that the event is structurally well-formed (every
 * REQUIRED attribute present); semantic and format validation is a separate step.
 */
@Suppress("TooManyFunctions") // A builder mirrors the full attribute set through two setter styles.
class CloudEventBuilder(val id: String, val source: String, val type: String) {
    var specVersion: SpecVersion = SpecVersion.V1_0
    var dataContentType: String? = null
    var dataContentEncoding: String? = null
    var dataSchema: String? = null
    var subject: String? = null
    var time: Instant? = null
    var data: CloudEventData? = null

    private val extensions = mutableMapOf<String, CloudEventAttributeValue>()

    fun withSpecVersion(specVersion: SpecVersion): CloudEventBuilder = apply { this.specVersion = specVersion }

    fun withDataContentType(dataContentType: String?): CloudEventBuilder = apply {
        this.dataContentType = dataContentType
    }

    /** Sets `datacontentencoding` (a CloudEvents v0.3 attribute; not valid under v1.0). */
    fun withDataContentEncoding(dataContentEncoding: String?): CloudEventBuilder = apply {
        this.dataContentEncoding = dataContentEncoding
    }

    fun withDataSchema(dataSchema: String?): CloudEventBuilder = apply { this.dataSchema = dataSchema }

    fun withSubject(subject: String?): CloudEventBuilder = apply { this.subject = subject }

    fun withTime(time: Instant?): CloudEventBuilder = apply { this.time = time }

    fun withData(data: CloudEventData?): CloudEventBuilder = apply { this.data = data }

    fun withData(bytes: ByteArray): CloudEventBuilder = apply { this.data = CloudEventData.wrap(bytes) }

    /** Sets an extension attribute, validating its [name] against the MUST naming rules. */
    fun extension(name: String, value: CloudEventAttributeValue): CloudEventBuilder = apply {
        extensions[AttributeNaming.requireValidName(name)] = value
    }

    fun extension(name: String, value: String): CloudEventBuilder = extension(name, StringValue(value))

    fun extension(name: String, value: Int): CloudEventBuilder = extension(name, IntegerValue(value))

    fun extension(name: String, value: Boolean): CloudEventBuilder = extension(name, BooleanValue(value))

    /** Removes a previously set extension attribute, if present. */
    fun removeExtension(name: String): CloudEventBuilder = apply { extensions.remove(name) }

    /**
     * Builds the immutable event.
     *
     * @throws IllegalArgumentException if any REQUIRED attribute (`id`, `source`, `type`) is unset.
     */
    fun build(): CloudEvent = CloudEvent(
        id = id,
        source = source,
        type = type,
        specVersion = specVersion,
        dataContentType = dataContentType,
        dataContentEncoding = dataContentEncoding,
        dataSchema = dataSchema,
        subject = subject,
        time = time,
        data = data,
        extensions = extensions.toMap(),
    )

    companion object {

        internal fun from(
            id: String? = null,
            source: String? = null,
            type: String? = null,
            event: CloudEvent,
        ): CloudEventBuilder = CloudEventBuilder(id ?: event.id, source ?: event.source, type ?: event.type).apply {
            specVersion = event.specVersion
            dataContentType = event.dataContentType
            dataContentEncoding = event.dataContentEncoding
            dataSchema = event.dataSchema
            subject = event.subject
            time = event.time
            data = event.data
            extensions.putAll(event.extensions)
        }
    }
}

/** Builds a [CloudEvent] with the DSL, e.g. `cloudEvent { id = "1"; source = "/s"; type = "t" }`. */
fun cloudEvent(id: String, source: String, type: String, block: CloudEventBuilder.() -> Unit = {}): CloudEvent =
    CloudEventBuilder(id, source, type).apply(block).build()

/** Derives a new event from this one, applying [block] to a builder seeded with its attributes. */
fun CloudEvent.copy(
    id: String? = null,
    source: String? = null,
    type: String? = null,
    block: CloudEventBuilder.() -> Unit = {},
): CloudEvent = CloudEventBuilder.from(id, source, type, this).apply(block).build()
