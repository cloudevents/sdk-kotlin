// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

import de.infix.testBalloon.framework.core.testSuite
import io.cloudevents.kotlin.core.BinaryValue
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.TimestampValue
import io.cloudevents.kotlin.core.UriReferenceValue
import io.cloudevents.kotlin.core.UriValue
import io.cloudevents.kotlin.core.cloudEvent
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToByteArray
import kotlin.time.Instant

private fun richV1() = cloudEvent("a234-1234-1234", "/sensors/tn-1234567", "com.example.temperature") {
    subject = "temperature"
    time = Instant.parse("2024-06-15T14:30:00.123456789Z")
    dataContentType = "text/plain"
    dataSchema = "https://schema.example.com/v1"
    data = ProtobufEventData.Text("23.5 degrees")
    extension("traceid", "abc-123")
    extension("retrycount", 3)
    extension("enabled", true)
    extension("coords", UriValue("https://example.com/coords/1"))
    extension("ref", UriReferenceValue("/relative/ref"))
    extension("checkedat", TimestampValue(Instant.parse("2024-06-15T14:00:00Z")))
    extension("blob", BinaryValue(byteArrayOf(1, 2, 3)))
}

private fun richV03() = cloudEvent("b234-1234-1234", "/sensors/tn-2345678", "com.example.pressure") {
    specVersion = SpecVersion.V0_3
    subject = "pressure"
    time = Instant.parse("2024-06-15T15:00:00Z")
    dataContentType = "application/octet-stream"
    dataSchema = "/schema/pressure/v1"
    dataContentEncoding = "base64"
    data = ProtobufEventData.Binary(byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()))
}

val protobufEventFormatRoundTripTest by testSuite("Protobuf event format — round-trip") {
    test("a rich v1.0 event round-trips with full semantic equality") {
        val event = richV1()
        val decoded = ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(event))
        assertEquals(event, decoded)
    }

    test("a rich v0.3 event round-trips with full semantic equality") {
        val event = richV03()
        val decoded = ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(event))
        assertEquals(event, decoded)
    }

    test("decode(encode(encode(decode(x)))) is stable — the codec is idempotent") {
        val first = ProtobufEventFormat.encodeToByteArray(richV1())
        val second = ProtobufEventFormat.encodeToByteArray(
            ProtobufEventFormat.decodeFromByteArray(first),
        )
        assertEquals(
            ProtobufEventFormat.decodeFromByteArray(first),
            ProtobufEventFormat.decodeFromByteArray(second),
            "re-encoding a decoded event must not change its semantics",
        )
    }

    test("text data stays text_data and binary data stays binary_data across a round-trip") {
        val original = richV1()
        val decoded = ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(original))
        assertIs<ProtobufEventData.Text>(decoded.data)
        assertEquals("23.5 degrees", (decoded.data as ProtobufEventData.Text).text)
    }

    test("proto (Any) data round-trips with type URL intact") {
        val event = cloudEvent("1", "http://localhost/source", "com.example.x") {
            dataContentType = "application/protobuf"
            data = ProtobufEventData.Proto("type.example.com/MyMessage", byteArrayOf(0x08, 0x2A))
        }
        val decoded = ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(event))
        val data = assertIs<ProtobufEventData.Proto>(decoded.data)
        assertEquals("type.example.com/MyMessage", data.typeUrl)
        assertTrue(data.value.contentEquals(byteArrayOf(0x08, 0x2A)))
    }

    test("v0.3 binary data stays binary_data (no base64 indirection in the protobuf format)") {
        val event = richV03()
        val decoded = ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(event))
        assertIs<ProtobufEventData.Binary>(decoded.data)
        assertEquals("base64", decoded.dataContentEncoding)
    }

    test("an event without data round-trips without a data member") {
        val event = cloudEvent("1", "http://localhost/source", "com.example.empty")
        val decoded = ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(event))
        assertEquals(event, decoded)
        assertNull(decoded.data)
    }
}

val protobufEventDataTest by testSuite("ProtobufEventData value semantics") {
    test("Binary equality is content-based") {
        assertEquals(ProtobufEventData.Binary(byteArrayOf(1, 2)), ProtobufEventData.Binary(byteArrayOf(1, 2)))
    }

    test("Proto equality is content-based and includes the type URL") {
        assertEquals(
            ProtobufEventData.Proto("t/x", byteArrayOf(1)),
            ProtobufEventData.Proto("t/x", byteArrayOf(1)),
        )
        kotlin.test.assertNotEquals(
            ProtobufEventData.Proto("t/x", byteArrayOf(1)),
            ProtobufEventData.Proto("t/y", byteArrayOf(1)),
        )
    }

    test("toBytes of an Any carrier returns the full Any wire encoding") {
        val data = ProtobufEventData.Proto("type.googleapis.com/x", byteArrayOf(1))
        val expectedAny = proto.encodeToByteArray(ProtoAny("type.googleapis.com/x", byteArrayOf(1)))
        assertTrue(data.toBytes().contentEquals(expectedAny))
    }

    test("carriers are usable as plain CloudEventData") {
        kotlin.test.assertNotNull(ProtobufEventData.Binary(byteArrayOf(1)).toBytes())
        assertEquals("hi", ProtobufEventData.Text("hi").toBytes().decodeToString())
    }
}