// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import io.cloudevents.kotlin.core.CloudEventData
import kotlinx.serialization.json.JsonElement

/**
 * The module-internal data-origin carrier (ADR 0005): the bridge between the wire JSON document and
 * the core [CloudEventData] payload, so encode and decode route `data` faithfully per §3.1.1 instead
 * of re-inferring intent from a bare byte array.
 *
 * The core model stores data as an opaque bytes-backed [CloudEventData]; this module pins finer origin
 * by wrapping the payload in one of these subclasses during decode (and, where the caller constructs an
 * event through core with a plain byte payload, [encodeFromPlainData] chooses the §3.1.1 route from the
 * `datacontenttype` alone). The subclasses are module-internal on purpose: they implement the public core
 * interface, so they flow through `CloudEvent` unchanged, but their concrete types never leak into the
 * module API.
 */
internal sealed class DataCarrier : CloudEventData

/**
 * The payload was an actual JSON value (object, array, or scalar) carried directly under `data`.
 * Re-encodes as the raw JSON value.
 */
internal class JsonData(val element: JsonElement) : DataCarrier() {
    override fun toBytes(): ByteArray = element.toString().encodeToByteArray()

    override fun equals(other: Any?): Boolean = other is JsonData && other.element == element

    override fun hashCode(): Int = element.hashCode()

    override fun toString(): String = "JsonData($element)"
}

/**
 * The payload was a JSON string under `data` for a non-JSON `datacontenttype`, held as its decoded text.
 * Re-encodes as a JSON string under `data`.
 */
internal class StringData(val text: String) : DataCarrier() {
    override fun toBytes(): ByteArray = text.encodeToByteArray()

    override fun equals(other: Any?): Boolean = other is StringData && other.text == text

    override fun hashCode(): Int = text.hashCode()

    override fun toString(): String = "StringData($text)"
}

/**
 * The payload was a Base64 (RFC 4648 §4) string decoded to bytes — a v1.0 `data_base64` member or a
 * v0.3 base64-encoded `data` member. Re-encodes as Base64 text under `data_base64` (v1.0) or under
 * `data` with `datacontentencoding: "base64"` (v0.3).
 */
internal class Base64Data(bytes: ByteArray) : DataCarrier() {
    private val content: ByteArray = bytes.copyOf()

    val bytes: ByteArray get() = content.copyOf()

    override fun toBytes(): ByteArray = content.copyOf()

    override fun equals(other: Any?): Boolean = other is Base64Data && other.content.contentEquals(content)

    override fun hashCode(): Int = content.contentHashCode()

    override fun toString(): String = "Base64Data(${content.size} bytes)"
}

/**
 * The `data` member was present and was the JSON literal `null`. Distinct from the *absence* of the
 * `data` member (which is `null` on the core event) so `{"data": null}` round-trips as an explicit
 * null rather than silently collapsing to "no data member".
 */
internal object NullData : DataCarrier() {
    override fun toBytes(): ByteArray = ByteArray(0)

    override fun toString(): String = "NullData"
}
