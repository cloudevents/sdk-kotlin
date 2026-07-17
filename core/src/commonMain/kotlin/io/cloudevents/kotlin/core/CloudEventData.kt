// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

/**
 * The domain-specific event payload, opaque to the core model.
 *
 * Core never parses or serializes the payload — its media type is named by
 * [CloudEvent.dataContentType] and its interpretation belongs to event-format modules. The seam
 * exposes only the raw bytes so future formats plug in without changing core.
 */
interface CloudEventData {
    /** Returns the payload as a byte array. Implementations must not expose internal mutable state. */
    fun toBytes(): ByteArray

    companion object {
        /** Wraps a byte array as opaque [CloudEventData]. The bytes are defensively copied. */
        fun wrap(bytes: ByteArray): CloudEventData = BytesCloudEventData(bytes)
    }
}

/**
 * The bytes-backed [CloudEventData] used by core. Equality is defined over the byte content so
 * two instances wrapping equal bytes are equal.
 */
private class BytesCloudEventData(bytes: ByteArray) : CloudEventData {
    private val bytes: ByteArray = bytes.copyOf()

    override fun toBytes(): ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BytesCloudEventData) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = "CloudEventData(${bytes.size} bytes)"
}
