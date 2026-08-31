// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

import de.infix.testBalloon.framework.core.testSuite
import io.cloudevents.kotlin.core.BinaryValue
import io.cloudevents.kotlin.core.CloudEvent
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.cloudEvent
import kotlin.io.encoding.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Vendored conformance fixtures — pinned source and provenance:
 *
 * The six sdk-java protobuf test fixtures — the `*.proto.json` files under
 * `formats/protobuf/src/test/resources/{v1,v03}` (Apache-2.0, from
 * https://github.com/cloudevents/sdk-java) are serialized to their canonical binary form with the
 * google reference implementation: the spec's own `cloudevents.proto` compiled with protoc 36.0
 * (`--java_out`) and parsed with `protobuf-java-util` 4.36.0's `JsonFormat.parser()`. The Base64
 * vectors below are those bytes, embedded so the suite runs identically on every KMP target.
 *
 * Cross-check: `v1_min` is byte-identical to the hand-computed golden used in the encode/decode
 * suites, so the golden's wire encoding is validated against the canonical implementation, and the
 * mirrored vectors validate decode against a corpus the reference SDK itself round-trips.
 */
private data class ProtoFixture(val name: String, val base64: String, val expected: CloudEvent)

private val sdkJavaFixtures = listOf(
    ProtoFixture(
        "v1_min",
        "CgExEhdodHRwOi8vbG9jYWxob3N0L3NvdXJjZRoDMS4wIgltb2NrLnRlc3Q=",
        cloudEvent("1", "http://localhost/source", "mock.test"),
    ),
    ProtoFixture(
        "v1_binary_ext",
        "CgExEhdodHRwOi8vbG9jYWxob3N0L3NvdXJjZRoDMS4wIgltb2NrLnRlc3QqEQoGYmluYXJ5EgciBeD/AESq",
        cloudEvent("1", "http://localhost/source", "mock.test") {
            extension("binary", BinaryValue(byteArrayOf(0xE0.toByte(), 0xFF.toByte(), 0x00, 0x44, 0xAA.toByte())))
        },
    ),
    ProtoFixture(
        "v1_json_data",
        "CgExEhdodHRwOi8vbG9jYWxob3N0L3NvdXJjZRoDMS4wIgltb2NrLnRlc3QqEAoEdGltZRIIOgYIiZWH1wUqJwoKZGF0YXNjaGVtYRIZKhdodHRwOi8vbG9jYWxob3N0L3NjaGVtYSolCg9kYXRhY29udGVudHR5cGUSEhoQYXBwbGljYXRpb24vanNvbioQCgdzdWJqZWN0EgUaA3N1YjoCe30=",
        cloudEvent("1", "http://localhost/source", "mock.test") {
            dataContentType = "application/json"
            dataSchema = "http://localhost/schema"
            subject = "sub"
            time = Instant.parse("2018-04-26T14:48:09+02:00")
            data = ProtobufEventData.Text("{}")
        },
    ),
    ProtoFixture(
        "v1_json_data_with_ext",
        "CgExEhdodHRwOi8vbG9jYWxob3N0L3NvdXJjZRoDMS4wIgltb2NrLnRlc3QqJwoKZGF0YXNjaGVtYRIZKhdodHRwOi8vbG9jYWxob3N0L3NjaGVtYSolCg9kYXRhY29udGVudHR5cGUSEhoQYXBwbGljYXRpb24vanNvbioQCgdzdWJqZWN0EgUaA3N1YioQCgR0aW1lEgg6BgiJlYfXBSoQCgdhc3RyaW5nEgUaA2FhYSoOCghhYm9vbGVhbhICCAEqDQoHYW51bWJlchICEAo6Ant9",
        cloudEvent("1", "http://localhost/source", "mock.test") {
            dataContentType = "application/json"
            dataSchema = "http://localhost/schema"
            subject = "sub"
            time = Instant.parse("2018-04-26T14:48:09+02:00")
            data = ProtobufEventData.Text("{}")
            extension("astring", "aaa")
            extension("aboolean", true)
            extension("anumber", 10)
        },
    ),
    ProtoFixture(
        "v1_text_data",
        "CgExEhdodHRwOi8vbG9jYWxob3N0L3NvdXJjZRoDMS4wIgltb2NrLnRlc3QqEAoEdGltZRIIOgYIiZWH1wUqEAoHc3ViamVjdBIFGgNzdWIqHwoPZGF0YWNvbnRlbnR0eXBlEgwaCnRleHQvcGxhaW46E0hlbGxvIFdvcmxkIExvcmVuYSE=",
        cloudEvent("1", "http://localhost/source", "mock.test") {
            dataContentType = "text/plain"
            subject = "sub"
            time = Instant.parse("2018-04-26T14:48:09+02:00")
            data = ProtobufEventData.Text("Hello World Lorena!")
        },
    ),
    ProtoFixture(
        "v1_xml_data",
        "CgExEhdodHRwOi8vbG9jYWxob3N0L3NvdXJjZRoDMS4wIgltb2NrLnRlc3QqEAoEdGltZRIIOgYIiZWH1wUqJAoPZGF0YWNvbnRlbnR0eXBlEhEaD2FwcGxpY2F0aW9uL3htbCoQCgdzdWJqZWN0EgUaA3N1YjoPPHN0dWZmPjwvc3R1ZmY+",
        cloudEvent("1", "http://localhost/source", "mock.test") {
            dataContentType = "application/xml"
            subject = "sub"
            time = Instant.parse("2018-04-26T14:48:09+02:00")
            data = ProtobufEventData.Text("<stuff></stuff>")
        },
    ),
    ProtoFixture(
        "v03_min",
        "CgExEhdodHRwOi8vbG9jYWxob3N0L3NvdXJjZRoDMC4zIgltb2NrLnRlc3Q=",
        cloudEvent("1", "http://localhost/source", "mock.test") {
            specVersion = SpecVersion.V0_3
        },
    ),
)

private fun decodeVector(base64: String): ByteArray = Base64.decode(base64)

val protobufConformanceSuite by testSuite("Protobuf event format — sdk-java mirrored conformance") {
    test("decodes every mirrored vector to the expected event") {
        for (fixture in sdkJavaFixtures) {
            val actual = ProtobufEventFormat.decodeFromByteArray(decodeVector(fixture.base64))
            assertEquals(
                fixture.expected,
                actual,
                "decode of ${fixture.name} does not match the expected event",
            )
        }
    }

    test("the round-trip property holds across the sdk-java mirrored vector set") {
        for (fixture in sdkJavaFixtures) {
            val first = ProtobufEventFormat.decodeFromByteArray(decodeVector(fixture.base64))
            val second = ProtobufEventFormat.decodeFromByteArray(ProtobufEventFormat.encodeToByteArray(first))
            assertEquals(first, second, "round-trip instability for ${fixture.name}")
        }
    }

    test("v1_min is byte-identical to the hand-computed golden (canonical encoder agreement)") {
        // The fixture's binary form produced by the google reference implementation equals the
        // hand-computed golden from the encode suite — validating both directions of the golden.
        val minimalEvent = cloudEvent("1", "http://localhost/source", "mock.test")
        assertContentEquals(decodeVector(sdkJavaFixtures[0].base64), ProtobufEventFormat.encodeToByteArray(minimalEvent))
    }
}