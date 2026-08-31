// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

import de.infix.testBalloon.framework.core.testSuite
import io.cloudevents.kotlin.core.BinaryValue
import io.cloudevents.kotlin.core.BooleanValue
import io.cloudevents.kotlin.core.IntegerValue
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.StringValue
import io.cloudevents.kotlin.core.TimestampValue
import io.cloudevents.kotlin.core.UriReferenceValue
import io.cloudevents.kotlin.core.UriValue
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToByteArray
import kotlin.time.Instant

// The same hand-computed vectors from the encode suite, consumed in the decode direction.
private val MINIMAL = byteArrayOf(
    0x0A, 0x01, 0x31, // id = "1"
    0x12, 0x17, 0x68, 0x74, 0x74, 0x70, 0x3A, 0x2F, 0x2F, 0x6C, 0x6F, 0x63, 0x61, 0x6C,
    0x68, 0x6F, 0x73, 0x74, 0x2F, 0x73, 0x6F, 0x75, 0x72, 0x63, 0x65, // source = "http://localhost/source"
    0x1A, 0x03, 0x31, 0x2E, 0x30, // spec_version = "1.0"
    0x22, 0x09, 0x6D, 0x6F, 0x63, 0x6B, 0x2E, 0x74, 0x65, 0x73, 0x74, // type = "mock.test"
)

private val FULL_ATTRS = MINIMAL + byteArrayOf(
    0x2A, 0x25, 0x0A, 0x0F, 0x64, 0x61, 0x74, 0x61, 0x63, 0x6F, 0x6E, 0x74, 0x65, 0x6E, 0x74, 0x74,
    0x79, 0x70, 0x65, 0x12, 0x12, 0x1A, 0x10, 0x61, 0x70, 0x70, 0x6C, 0x69, 0x63, 0x61, 0x74, 0x69,
    0x6F, 0x6E, 0x2F, 0x6A, 0x73, 0x6F, 0x6E,
)

private val INT_EXT = MINIMAL + byteArrayOf(0x2A, 0x0B, 0x0A, 0x05, 0x65, 0x78, 0x69, 0x6E, 0x74, 0x12, 0x02, 0x10, 0x2A)

private val TIMESTAMP_ATTR = MINIMAL + byteArrayOf(
    // proto3 omits default values, so a zero nanos field is absent — matching the canonical encoder.
    0x2A, 0x10, 0x0A, 0x04, 0x74, 0x69, 0x6D, 0x65, 0x12, 0x08, 0x3A, 0x06, 0x08, 0xE8.toByte(), 0xCA.toByte(), 0xB6.toByte(), 0xB3.toByte(), 0x06,
)

private val BINARY_DATA = MINIMAL + byteArrayOf(0x32, 0x03, 0x01, 0x02, 0x03)

private val TEXT_DATA = MINIMAL + byteArrayOf(
    0x3A, 0x0D, 0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x2C, 0x20, 0x57, 0x6F, 0x72, 0x6C, 0x64, 0x21,
)

private val PROTO_DATA = MINIMAL + byteArrayOf(
    0x42, 0x1A, 0x0A, 0x15, 0x74, 0x79, 0x70, 0x65, 0x2E, 0x67, 0x6F, 0x6F, 0x67, 0x6C, 0x65, 0x61,
    0x70, 0x69, 0x73, 0x2E, 0x63, 0x6F, 0x6D, 0x2F, 0x78, 0x12, 0x01, 0x01,
)

val protobufEventFormatDecodeTest by testSuite("Protobuf event format — decode") {
    test("decodes a minimal event from the hand-computed golden bytes") {
        val event = ProtobufEventFormat.decodeFromByteArray(MINIMAL)
        assertEquals("1", event.id)
        assertEquals("http://localhost/source", event.source)
        assertEquals("mock.test", event.type)
        assertEquals(SpecVersion.V1_0, event.specVersion)
        assertNull(event.data)
        assertTrue(event.extensions.isEmpty())
    }

    test("decodes datacontenttype from a ce_string map entry") {
        val event = ProtobufEventFormat.decodeFromByteArray(FULL_ATTRS)
        assertEquals("application/json", event.dataContentType)
    }

    test("decodes an Integer extension from a ce_integer map entry") {
        val event = ProtobufEventFormat.decodeFromByteArray(INT_EXT)
        assertEquals(IntegerValue(42), event.getExtension("exint"))
    }

    test("decodes time from a ce_timestamp map entry") {
        val event = ProtobufEventFormat.decodeFromByteArray(TIMESTAMP_ATTR)
        assertEquals(Instant.parse("2024-06-15T14:30:00Z"), event.time)
    }

    test("decodes binary_data to a Binary carrier") {
        val event = ProtobufEventFormat.decodeFromByteArray(BINARY_DATA)
        val data = assertIs<ProtobufEventData.Binary>(event.data)
        assertContentEquals(byteArrayOf(1, 2, 3), data.bytes)
    }

    test("decodes text_data to a Text carrier") {
        val event = ProtobufEventFormat.decodeFromByteArray(TEXT_DATA)
        assertEquals(ProtobufEventData.Text("Hello, World!"), event.data)
    }

    test("decodes proto_data to a Proto carrier with type URL and value bytes") {
        val event = ProtobufEventFormat.decodeFromByteArray(PROTO_DATA)
        val data = assertIs<ProtobufEventData.Proto>(event.data)
        assertEquals("type.googleapis.com/x", data.typeUrl)
        assertContentEquals(byteArrayOf(1), data.value)
    }

    test("rejects an unknown specversion") {
        val wire = MINIMAL.toMutableList().apply { this[30] = 0x39 } // "1.0" -> "9.0"
        assertFailsWith<ProtobufEventFormatException> {
            ProtobufEventFormat.decodeFromByteArray(wire.toByteArray())
        }
    }

    test("rejects an empty document (missing required attributes)") {
        assertFailsWith<ProtobufEventFormatException> {
            ProtobufEventFormat.decodeFromByteArray(ByteArray(0))
        }
    }

    test("rejects unparseable bytes") {
        assertFailsWith<ProtobufEventFormatException> {
            ProtobufEventFormat.decodeFromByteArray(byteArrayOf(0xFF.toByte()))
        }
    }

    test("data oneof resolves to the last wire member (proto3 last-wins)") {
        // A conformant protobuf parser keeps only the last `oneof data` member seen; distinct
        // branches decode to separate nullable fields through kotlinx, so the wire-order scan
        // selects text_data here (it follows binary_data).
        val binaryThenText = BINARY_DATA + TEXT_DATA.copyOfRange(TEXT_DATA.size - 15, TEXT_DATA.size)
        val winsText = ProtobufEventFormat.decodeFromByteArray(binaryThenText)
        assertEquals(ProtobufEventData.Text("Hello, World!"), winsText.data)

        val textThenBinary = TEXT_DATA + BINARY_DATA.copyOfRange(BINARY_DATA.size - 5, BINARY_DATA.size)
        val winsBinary = ProtobufEventFormat.decodeFromByteArray(textThenBinary)
        assertEquals(ProtobufEventData.Binary(byteArrayOf(1, 2, 3)), winsBinary.data)
    }

    test("attr oneof resolves to the last wire member (proto3 last-wins)") {
        // ce_integer (field 2) is emitted before ce_string (field 3), so the string branch wins.
        val wire = proto.encodeToByteArray(
            ProtoCloudEvent(
                id = "1",
                source = "http://localhost/source",
                specVersion = "1.0",
                type = "mock.test",
                attributes = mapOf(
                    "ambiguous" to ProtoAttributeValue(ceInteger = 42, ceString = "forty-two"),
                ),
            ),
        )
        val event = ProtobufEventFormat.decodeFromByteArray(wire)
        assertEquals(StringValue("forty-two"), event.getExtension("ambiguous"))
    }

    test("truncated length-delimited data is wrapped as a format exception") {
        // kotlinx protobuf raises a raw IndexOutOfBoundsException for a string field whose declared
        // length exceeds the remaining bytes; the public contract requires ProtobufEventFormatException.
        val truncated = MINIMAL + byteArrayOf(0x3A, 0x0D, 0x48, 0x65, 0x6C, 0x6C, 0x6F) // declares 13 bytes, has 5
        val e = assertFailsWith<ProtobufEventFormatException> {
            ProtobufEventFormat.decodeFromByteArray(truncated)
        }
        kotlin.test.assertTrue(e.message.orEmpty().contains("truncated"))
    }

    test("omits an attribute whose oneof is completely unset (ATTR_NOT_SET analog)") {
        val wire = proto.encodeToByteArray(
            ProtoCloudEvent(
                id = "1",
                source = "http://localhost/source",
                specVersion = "1.0",
                type = "mock.test",
                attributes = mapOf("unset" to ProtoAttributeValue()),
            ),
        )
        val event = ProtobufEventFormat.decodeFromByteArray(wire)
        assertTrue(event.extensions.isEmpty())
    }

    test("rejects a reserved member in the attributes map") {
        val wire = proto.encodeToByteArray(
            ProtoCloudEvent(
                id = "1",
                source = "http://localhost/source",
                specVersion = "1.0",
                type = "mock.test",
                attributes = mapOf("id" to ProtoAttributeValue(ceString = "hijacked")),
            ),
        )
        assertFailsWith<ProtobufEventFormatException> { ProtobufEventFormat.decodeFromByteArray(wire) }
    }

    test("routes a wrong-version optional member to an extension (dataschema under v0.3)") {
        // v0.3 dropped the absolute-URI type, so the wire carries the wrong-version name as a plain
        // string; it must surface as an extension of the String type, not as a core attribute.
        val wire = proto.encodeToByteArray(
            ProtoCloudEvent(
                id = "1",
                source = "http://localhost/source",
                specVersion = "0.3",
                type = "mock.test",
                attributes = mapOf("dataschema" to ProtoAttributeValue(ceString = "http://schema.example.com/v1")),
            ),
        )
        val event = ProtobufEventFormat.decodeFromByteArray(wire)
        assertEquals(StringValue("http://schema.example.com/v1"), event.getExtension("dataschema"))
        assertNull(event.dataSchema)
    }

    test("decodes every extension type to its oneof-faithful type-system value") {
        val wire = proto.encodeToByteArray(
            ProtoCloudEvent(
                id = "1",
                source = "http://localhost/source",
                specVersion = "1.0",
                type = "mock.test",
                attributes = mapOf(
                    "exbool" to ProtoAttributeValue(ceBoolean = true),
                    "exint" to ProtoAttributeValue(ceInteger = 7),
                    "exstr" to ProtoAttributeValue(ceString = "hello"),
                    "exbytes" to ProtoAttributeValue(ceBytes = byteArrayOf(9, 9)),
                    "exuri" to ProtoAttributeValue(ceUri = "https://example.com/x"),
                    "exuriref" to ProtoAttributeValue(ceUriRef = "/relative"),
                    "exts" to ProtoAttributeValue(ceTimestamp = ProtoTimestamp(seconds = 0, nanos = 0)),
                ),
            ),
        )
        val event = ProtobufEventFormat.decodeFromByteArray(wire)
        assertEquals(BooleanValue(true), event.getExtension("exbool"))
        assertEquals(IntegerValue(7), event.getExtension("exint"))
        assertEquals(StringValue("hello"), event.getExtension("exstr"))
        assertEquals(BinaryValue(byteArrayOf(9, 9)), event.getExtension("exbytes"))
        assertEquals(UriValue("https://example.com/x"), event.getExtension("exuri"))
        assertEquals(UriReferenceValue("/relative"), event.getExtension("exuriref"))
        assertEquals(TimestampValue(Instant.fromEpochSeconds(0, 0)), event.getExtension("exts"))
    }

    test("rejects an out-of-range protobuf Timestamp nanos") {
        val wire = proto.encodeToByteArray(
            ProtoCloudEvent(
                id = "1",
                source = "http://localhost/source",
                specVersion = "1.0",
                type = "mock.test",
                attributes = mapOf(
                    "time" to ProtoAttributeValue(ceTimestamp = ProtoTimestamp(seconds = 0, nanos = 1_000_000_000)),
                ),
            ),
        )
        assertFailsWith<ProtobufEventFormatException> { ProtobufEventFormat.decodeFromByteArray(wire) }
    }

    test("rejects an attribute with the wrong type for its context attribute") {
        val wire = proto.encodeToByteArray(
            ProtoCloudEvent(
                id = "1",
                source = "http://localhost/source",
                specVersion = "1.0",
                type = "mock.test",
                attributes = mapOf("datacontenttype" to ProtoAttributeValue(ceInteger = 5)),
            ),
        )
        assertFailsWith<ProtobufEventFormatException> { ProtobufEventFormat.decodeFromByteArray(wire) }
    }

    test("validates semantically and rejects an invalid event") {
        // source must be a valid URI-reference; this one contains a space.
        val wire = proto.encodeToByteArray(
            ProtoCloudEvent(
                id = "1",
                source = "not a uri",
                specVersion = "1.0",
                type = "mock.test",
            ),
        )
        assertFailsWith<ProtobufEventFormatException> { ProtobufEventFormat.decodeFromByteArray(wire) }
    }
}