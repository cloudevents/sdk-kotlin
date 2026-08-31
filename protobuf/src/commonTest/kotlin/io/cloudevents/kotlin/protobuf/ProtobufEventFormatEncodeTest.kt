// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

import de.infix.testBalloon.framework.core.testSuite
import io.cloudevents.kotlin.core.CloudEventData
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.cloudEvent
import io.cloudevents.kotlin.core.copy
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToByteArray
import kotlin.time.Instant

// Hand-computed golden wire bytes (produced with an independent protobuf wire-format encoder,
// then verified field-by-field against `cloudevents.proto`; see ProtobufEventFormatDecodeTest for
// the decode direction of the same vectors).
private val MINIMAL = byteArrayOf(
    0x0A, 0x01, 0x31, // id = "1"
    0x12, 0x17, 0x68, 0x74, 0x74, 0x70, 0x3A, 0x2F, 0x2F, 0x6C, 0x6F, 0x63, 0x61, 0x6C,
    0x68, 0x6F, 0x73, 0x74, 0x2F, 0x73, 0x6F, 0x75, 0x72, 0x63, 0x65, // source = "http://localhost/source"
    0x1A, 0x03, 0x31, 0x2E, 0x30, // spec_version = "1.0"
    0x22, 0x09, 0x6D, 0x6F, 0x63, 0x6B, 0x2E, 0x74, 0x65, 0x73, 0x74, // type = "mock.test"
)

private val FULL_ATTRS = MINIMAL + byteArrayOf(
    0x2A, 0x25, // attributes map entry: datacontenttype → ce_string "application/json"
    0x0A, 0x0F, 0x64, 0x61, 0x74, 0x61, 0x63, 0x6F, 0x6E, 0x74, 0x65, 0x6E, 0x74, 0x74, 0x79, 0x70, 0x65,
    0x12, 0x12, 0x1A, 0x10, 0x61, 0x70, 0x70, 0x6C, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6F, 0x6E, 0x2F, 0x6A, 0x73, 0x6F, 0x6E,
)

private val INT_EXT = MINIMAL + byteArrayOf(
    0x2A, 0x0B, // attributes map entry: exint → ce_integer 42
    0x0A, 0x05, 0x65, 0x78, 0x69, 0x6E, 0x74,
    0x12, 0x02, 0x10, 0x2A,
)

private val TIMESTAMP_ATTR = MINIMAL + byteArrayOf(
    0x2A, 0x10, // attributes map entry: time → ce_timestamp 2024-06-15T14:30:00Z
    0x0A, 0x04, 0x74, 0x69, 0x6D, 0x65,
    // proto3 omits default values, so a zero nanos field is absent — matching the canonical encoder.
    0x12, 0x08, 0x3A, 0x06, 0x08, 0xE8.toByte(), 0xCA.toByte(), 0xB6.toByte(), 0xB3.toByte(), 0x06,
)

private val BINARY_DATA = MINIMAL + byteArrayOf(0x32, 0x03, 0x01, 0x02, 0x03) // binary_data = [1, 2, 3]

private val TEXT_DATA = MINIMAL + byteArrayOf(
    0x3A, 0x0D, 0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x2C, 0x20, 0x57, 0x6F, 0x72, 0x6C, 0x64, 0x21, // "Hello, World!"
)

private val PROTO_DATA = MINIMAL + byteArrayOf(
    0x42, 0x1A, // proto_data = Any { type_url = "type.googleapis.com/x", value = [1] }
    0x0A, 0x15, 0x74, 0x79, 0x70, 0x65, 0x2E, 0x67, 0x6F, 0x6F, 0x67, 0x6C, 0x65, 0x61, 0x70, 0x69,
    0x73, 0x2E, 0x63, 0x6F, 0x6D, 0x2F, 0x78,
    0x12, 0x01, 0x01,
)

private val minimalEvent = cloudEvent("1", "http://localhost/source", "mock.test")

val protobufEventFormatEncodeTest by testSuite("Protobuf event format — encode") {
    test("encodes a minimal event to the hand-computed golden wire bytes") {
        assertContentEquals(MINIMAL, ProtobufEventFormat.encodeToByteArray(minimalEvent))
    }

    test("encodes datacontenttype to a ce_string map entry") {
        val event = cloudEvent("1", "http://localhost/source", "mock.test") {
            dataContentType = "application/json"
        }
        assertContentEquals(FULL_ATTRS, ProtobufEventFormat.encodeToByteArray(event))
    }

    test("encodes an Integer extension to a ce_integer map entry") {
        val event = cloudEvent("1", "http://localhost/source", "mock.test") {
            extension("exint", 42)
        }
        assertContentEquals(INT_EXT, ProtobufEventFormat.encodeToByteArray(event))
    }

    test("encodes time to a ce_timestamp map entry") {
        val event = cloudEvent("1", "http://localhost/source", "mock.test") {
            time = Instant.parse("2024-06-15T14:30:00Z")
        }
        assertContentEquals(TIMESTAMP_ATTR, ProtobufEventFormat.encodeToByteArray(event))
    }

    test("routes an explicit binary carrier to binary_data") {
        val event = minimalEvent.copy { data = ProtobufEventData.Binary(byteArrayOf(1, 2, 3)) }
        assertContentEquals(BINARY_DATA, ProtobufEventFormat.encodeToByteArray(event))
    }

    test("routes an explicit text carrier to text_data") {
        val event = minimalEvent.copy { data = ProtobufEventData.Text("Hello, World!") }
        assertContentEquals(TEXT_DATA, ProtobufEventFormat.encodeToByteArray(event))
    }

    test("routes an Any carrier to proto_data") {
        val event = minimalEvent.copy {
            data = ProtobufEventData.Proto("type.googleapis.com/x", byteArrayOf(1))
        }
        assertContentEquals(PROTO_DATA, ProtobufEventFormat.encodeToByteArray(event))
    }

    test("routes plain core bytes to binary_data") {
        val event = minimalEvent.copy { data = CloudEventData.wrap(byteArrayOf(1, 2, 3)) }
        assertContentEquals(BINARY_DATA, ProtobufEventFormat.encodeToByteArray(event))
    }

    test("routes a text-declared payload to text_data") {
        val event = minimalEvent.copy {
            dataContentType = "text/plain"
            data = CloudEventData.wrap("Hello, World!".encodeToByteArray())
        }
        val decoded = ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(event))
        assertEquals("text/plain", decoded.dataContentType)
        assertEquals(ProtobufEventData.Text("Hello, World!"), decoded.data)
    }

    test("routes a protobuf-declared payload through Any") {
        val anyBytes = proto.encodeToByteArray(ProtoAny("type.googleapis.com/x", byteArrayOf(1)))
        val event = minimalEvent.copy {
            dataContentType = "application/protobuf"
            data = CloudEventData.wrap(anyBytes)
        }
        val decoded = ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(event))
        assertEquals("application/protobuf", decoded.dataContentType)
        assertEquals(ProtobufEventData.Proto("type.googleapis.com/x", byteArrayOf(1)), decoded.data)
    }

    test("the document media type is not a data content type (routes to binary_data)") {
        // application/cloudevents+protobuf identifies the structured event document, not a payload
        // type; a plain payload carrying it as datacontenttype is not an Any and goes to binary_data.
        val event = minimalEvent.copy {
            dataContentType = "application/cloudevents+protobuf"
            data = CloudEventData.wrap(byteArrayOf(1, 2, 3))
        }
        val decoded = ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(event))
        assertEquals(ProtobufEventData.Binary(byteArrayOf(1, 2, 3)), decoded.data)
    }

    test("rejects a protobuf-declared payload that is not a valid Any") {
        val event = minimalEvent.copy {
            dataContentType = "application/protobuf"
            data = CloudEventData.wrap(byteArrayOf(0x01))
        }
        assertFailsWith<ProtobufEventFormatException> { ProtobufEventFormat.encodeToByteArray(event) }
    }

    test("wraps a truncated protobuf-declared payload as a format exception") {
        // The payload claims to be a google.protobuf.Any: type_url (field 1) declares 5 bytes but
        // only 1 follows, so kotlinx protobuf raises a raw IndexOutOfBoundsException inside
        // parseAnyData; the public contract requires ProtobufEventFormatException.
        val event = minimalEvent.copy {
            dataContentType = "application/protobuf"
            data = CloudEventData.wrap(byteArrayOf(0x0A, 0x05, 0x41))
        }
        val e = assertFailsWith<ProtobufEventFormatException> {
            ProtobufEventFormat.encodeToByteArray(event)
        }
        kotlin.test.assertTrue(e.message.orEmpty().contains("truncated"))
    }

    test("rejects a text-declared payload that is not valid UTF-8") {
        val event = minimalEvent.copy {
            dataContentType = "text/plain"
            data = CloudEventData.wrap(byteArrayOf(0xC3.toByte(), 0x28)) // invalid continuation byte
        }
        assertFailsWith<ProtobufEventFormatException> { ProtobufEventFormat.encodeToByteArray(event) }
    }

    test("v0.3 encodes schemaurl and datacontentencoding as map entries and keeps binary as binary_data") {
        val event = cloudEvent("1", "http://localhost/source", "mock.test") {
            specVersion = SpecVersion.V0_3
            dataSchema = "http://schema.example.com/v1"
            dataContentEncoding = "base64"
            data = ProtobufEventData.Binary(byteArrayOf(1, 2, 3))
        }
        val encoded = ProtobufEventFormat.encodeToByteArray(event)
        val decoded = ProtobufEventFormat.decodeFromByteArray(encoded)
        assertEquals(event, decoded)
        assertEquals(SpecVersion.V0_3, decoded.specVersion)
        assertEquals("http://schema.example.com/v1", decoded.dataSchema)
        assertEquals("base64", decoded.dataContentEncoding)
        assertTrue(decoded.data is ProtobufEventData.Binary)
    }

    test("times with sub-second precision survive the seconds+nanos split") {
        val instant = Instant.parse("2024-06-15T14:30:00.123456789Z")
        val event = cloudEvent("1", "http://localhost/source", "mock.test") { time = instant }
        val decoded = ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(event))
        assertEquals(instant, decoded.time)
    }

    test("timestamp attributes encode at both inclusive bounds of Google's well-formed range") {
        val min = Instant.fromEpochSeconds(-62_135_596_800L) // 0001-01-01T00:00:00Z
        val max = Instant.fromEpochSeconds(253_402_300_799L) // 9999-12-31T23:59:59Z
        val minEvent = cloudEvent("1", "http://localhost/source", "mock.test") { time = min }
        val maxEvent = cloudEvent("1", "http://localhost/source", "mock.test") { time = max }
        assertEquals(
            min,
            ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(minEvent)).time,
        )
        assertEquals(
            max,
            ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(maxEvent)).time,
        )
    }

    test("rejects timestamp attributes outside Google's well-formed range on encode") {
        val beforeYearOne = cloudEvent("1", "http://localhost/source", "mock.test") {
            time = Instant.fromEpochSeconds(-62_135_596_801L) // year 0000
        }
        val afterYear9999 = cloudEvent("1", "http://localhost/source", "mock.test") {
            time = Instant.fromEpochSeconds(253_402_300_800L) // year 10000
        }
        assertFailsWith<ProtobufEventFormatException> {
            ProtobufEventFormat.encodeToByteArray(beforeYearOne)
        }
        assertFailsWith<ProtobufEventFormatException> {
            ProtobufEventFormat.encodeToByteArray(afterYear9999)
        }
    }

    test("isTextContent mirrors the reference rules and tolerates parameters") {
        assertTrue(isTextContent("text/plain"))
        assertTrue(isTextContent("text/plain; charset=utf-8"))
        assertTrue(isTextContent("application/json"))
        assertTrue(isTextContent("application/cloudevents+json"))
        assertTrue(isTextContent("application/xml"))
        kotlin.test.assertFalse(isTextContent("application/octet-stream"))
        kotlin.test.assertFalse(isTextContent(null))
    }
}