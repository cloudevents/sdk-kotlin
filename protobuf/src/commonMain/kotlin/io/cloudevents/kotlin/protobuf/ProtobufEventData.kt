// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

import io.cloudevents.kotlin.core.CloudEventData
import kotlinx.serialization.encodeToByteArray

/**
 * The protobuf module's data-origin carriers: the bridge between the wire `oneof data` and the
 * core [CloudEventData] payload, so encode and decode route `data` faithfully instead of
 * re-inferring intent from a bare byte array (the protobuf counterpart of the JSON module's
 * ADR 0005 carrier).
 *
 * Unlike the JSON module's internal carriers, these are public because the `oneof data` is part of
 * the format's public contract ([ProtobufEventData.Proto] lets callers hand the codec an `Any`
 * payload — the "full oneof" story). They implement the public core interface, so they flow through
 * `CloudEvent` unchanged.
 */
public sealed interface ProtobufEventData : CloudEventData {
    /** The payload was carried in `binary_data`. Re-encodes as `binary_data`. */
    public class Binary(bytes: ByteArray) : ProtobufEventData {
        private val content: ByteArray = bytes.copyOf()

        public val bytes: ByteArray get() = content.copyOf()

        override fun toBytes(): ByteArray = content.copyOf()

        override fun equals(other: Any?): Boolean = other is Binary && other.content.contentEquals(content)

        override fun hashCode(): Int = content.contentHashCode()

        override fun toString(): String = "ProtobufEventData.Binary(${content.size} bytes)"
    }

    /** The payload was carried as `text_data` text. Re-encodes as `text_data`. */
    public data class Text(public val text: String) : ProtobufEventData {
        override fun toBytes(): ByteArray = text.encodeToByteArray()
    }

    /**
     * The payload was carried as `proto_data`, a `google.protobuf.Any` (type URL + opaque value
     * bytes). Re-encodes as `proto_data` with the same type URL.
     */
    public class Proto(public val typeUrl: String, value: ByteArray) : ProtobufEventData {
        private val content: ByteArray = value.copyOf()

        public val value: ByteArray get() = content.copyOf()

        override fun toBytes(): ByteArray = encodeToAnyBytes(typeUrl, content)

        override fun equals(other: Any?): Boolean =
            other is Proto && other.typeUrl == typeUrl && other.content.contentEquals(content)

        override fun hashCode(): Int = 31 * typeUrl.hashCode() + content.contentHashCode()

        override fun toString(): String = "ProtobufEventData.Proto(typeUrl=$typeUrl, ${content.size} bytes)"
    }
}

/** Wire-encodes an `Any` message (type_url field 1 + value field 2) for [ProtobufEventData.Proto.toBytes]. */
internal fun encodeToAnyBytes(typeUrl: String, value: ByteArray): ByteArray =
    proto.encodeToByteArray(ProtoAny(typeUrl = typeUrl, value = value))
