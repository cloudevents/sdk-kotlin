// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

/**
 * Minimal protobuf wire-order scanner.
 *
 * Proto3 `oneof` semantics say a parser retains the LAST wire member of the group. kotlinx
 * serialization decodes every `oneof` branch (modeled as nullable fields) without tracking wire
 * order, so the model cannot distinguish `binary_data`-then-`text_data` from its reverse. This
 * scanner reads the raw bytes once per document to recover that order, for the two oneof groups in
 * `cloudevents.proto`: the top-level `data` oneof (fields 6-8) and the nested `attr` oneof
 * (fields 1-7 of each `CloudEventAttributeValue` map value). Decoding then applies the last-seen
 * field, matching a conformant parser (and the reference protobuf-java implementation).
 *
 * The scanner only walks structure it must skip or select; it never interprets payload content.
 * Anything not well-formed by the protobuf wire format is reported as malformed.
 */
internal object WireScan {
    private const val WIRE_VARINT = 0
    private const val WIRE_FIXED64 = 1
    private const val WIRE_LENGTH = 2
    private const val WIRE_FIXED32 = 5
    private const val FIXED64_SIZE = 8
    private const val FIXED32_SIZE = 4
    private const val FIELD_SHIFT = 3
    private const val WIRE_TYPE_MASK = 0x7
    private const val VARINT_VALUE_MASK = 0x7F
    private const val VARINT_CONTINUATION = 0x80
    private const val VARINT_SHIFT_STEP = 7
    private const val VARINT_MAX_SHIFT = 64
    private const val BYTE_MASK = 0xFF

    /** The wire-order outcome for one event message: which `oneof` members were last seen. */
    internal data class EventScan(val lastDataField: Int? = null, val lastAttrField: Map<String, Int> = emptyMap())

    private data class Var(val value: Long, val size: Int)

    private data class Tag(val field: Int, val wire: Int, val size: Int)

    /** Scans one structured-mode event message, returning the last-seen `oneof` members. */
    internal fun scanEvent(bytes: ByteArray): EventScan {
        val lastAttr = LinkedHashMap<String, Int>()
        var lastData: Int? = null
        var pos = 0
        while (pos < bytes.size) {
            val tag = tagInfo(bytes, pos)
            pos += tag.size
            when (tag.wire) {
                WIRE_VARINT -> pos = skipVarint(bytes, pos)
                WIRE_FIXED64 -> pos = skipFixed(bytes, pos, FIXED64_SIZE)
                WIRE_FIXED32 -> pos = skipFixed(bytes, pos, FIXED32_SIZE)
                WIRE_LENGTH -> {
                    val len = readVarint(bytes, pos)
                    pos += len.size
                    if (pos + len.value > bytes.size) malformed()
                    if (tag.field == WireFields.ATTRIBUTES) {
                        scanMapEntry(bytes, pos, (pos + len.value).toInt(), lastAttr)
                    } else if (tag.field in WireFields.BINARY_DATA..WireFields.PROTO_DATA) {
                        lastData = tag.field
                    }
                    pos = (pos + len.value).toInt()
                }
                else -> malformed()
            }
        }
        return EventScan(lastDataField = lastData, lastAttrField = lastAttr)
    }

    /** Splits a `CloudEventBatch` message into its repeated `events` (field 1) segments. */
    internal fun splitBatch(bytes: ByteArray): List<ByteArray> {
        val segments = mutableListOf<ByteArray>()
        var pos = 0
        while (pos < bytes.size) {
            val tag = tagInfo(bytes, pos)
            pos += tag.size
            when (tag.wire) {
                WIRE_LENGTH -> {
                    val len = readVarint(bytes, pos)
                    pos += len.size
                    if (pos + len.value > bytes.size) malformed()
                    if (tag.field == 1) {
                        segments.add(bytes.copyOfRange(pos, (pos + len.value).toInt()))
                    }
                    pos = (pos + len.value).toInt()
                }
                WIRE_VARINT -> pos = skipVarint(bytes, pos)
                WIRE_FIXED64 -> pos = skipFixed(bytes, pos, FIXED64_SIZE)
                WIRE_FIXED32 -> pos = skipFixed(bytes, pos, FIXED32_SIZE)
                else -> malformed()
            }
        }
        return segments
    }

    /** Scans one `map<string, CloudEventAttributeValue>` entry: key (field 1) + value (field 2). */
    private fun scanMapEntry(bytes: ByteArray, start: Int, end: Int, lastAttr: MutableMap<String, Int>) {
        var key: String? = null
        var valueStart = -1
        var valueEnd = -1
        var pos = start
        while (pos < end) {
            val tag = tagInfo(bytes, pos)
            pos += tag.size
            when (tag.wire) {
                WIRE_LENGTH -> {
                    val len = readVarint(bytes, pos)
                    pos += len.size
                    if (pos + len.value > end) malformed()
                    when (tag.field) {
                        1 -> key = readString(bytes, pos, (pos + len.value).toInt())
                        2 -> {
                            valueStart = pos
                            valueEnd = (pos + len.value).toInt()
                        }
                    }
                    pos = (pos + len.value).toInt()
                }
                WIRE_VARINT -> pos = skipVarint(bytes, pos)
                WIRE_FIXED64 -> pos = skipFixed(bytes, pos, FIXED64_SIZE)
                WIRE_FIXED32 -> pos = skipFixed(bytes, pos, FIXED32_SIZE)
                else -> malformed()
            }
        }
        if (key != null && valueStart >= 0) {
            val resolvedKey = key
            lastAttr[resolvedKey] = lastAttrOneofField(bytes, valueStart, valueEnd)
        }
    }

    /** Returns the last-seen field in the `attr` oneof range inside a value message, or -1 if none. */
    private fun lastAttrOneofField(bytes: ByteArray, start: Int, end: Int): Int {
        var last = -1
        var pos = start
        while (pos < end) {
            val tag = tagInfo(bytes, pos)
            pos += tag.size
            if (tag.field in WireFields.CE_BOOLEAN..WireFields.CE_TIMESTAMP) {
                last = tag.field
            }
            when (tag.wire) {
                WIRE_VARINT -> pos = skipVarint(bytes, pos)
                WIRE_FIXED64 -> pos = skipFixed(bytes, pos, FIXED64_SIZE)
                WIRE_FIXED32 -> pos = skipFixed(bytes, pos, FIXED32_SIZE)
                WIRE_LENGTH -> {
                    val len = readVarint(bytes, pos)
                    pos += len.size
                    if (pos + len.value > end) malformed()
                    pos = (pos + len.value).toInt()
                }
                else -> malformed()
            }
        }
        return last
    }

    private fun tagInfo(bytes: ByteArray, pos: Int): Tag {
        val v = readVarint(bytes, pos)
        return Tag((v.value ushr FIELD_SHIFT).toInt(), (v.value and WIRE_TYPE_MASK.toLong()).toInt(), v.size)
    }

    private fun readVarint(bytes: ByteArray, pos: Int): Var {
        var value = 0L
        var shift = 0
        var index = pos
        while (index < bytes.size && shift < VARINT_MAX_SHIFT) {
            val b = bytes[index].toInt() and BYTE_MASK
            value = value or ((b and VARINT_VALUE_MASK).toLong() shl shift)
            if (b and VARINT_CONTINUATION == 0) return Var(value, index - pos + 1)
            shift += VARINT_SHIFT_STEP
            index++
        }
        malformed()
    }

    private fun skipVarint(bytes: ByteArray, pos: Int): Int {
        var index = pos
        while (index < bytes.size) {
            if (bytes[index].toInt() and VARINT_CONTINUATION == 0) return index + 1
            index++
        }
        malformed()
    }

    private fun skipFixed(bytes: ByteArray, pos: Int, width: Int): Int {
        if (pos + width > bytes.size) malformed()
        return pos + width
    }

    private fun readString(bytes: ByteArray, start: Int, end: Int): String = try {
        bytes.copyOfRange(start, end).decodeToString(throwOnInvalidSequence = true)
    } catch (e: kotlin.text.CharacterCodingException) {
        malformed(e)
    }

    private fun malformed(cause: Throwable? = null): Nothing =
        throw ProtobufEventFormatException("Cannot parse document as protobuf (malformed or truncated message)", cause)
}
