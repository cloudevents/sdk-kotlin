// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

import de.infix.testBalloon.framework.core.testSuite
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.cloudEvent
import io.cloudevents.kotlin.core.copy
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val miniA = cloudEvent("a", "http://localhost/a", "com.example.a")
private val miniB = cloudEvent("b", "http://localhost/b", "com.example.b") {
    specVersion = SpecVersion.V0_3
    subject = "batch-subject"
}

val protobufEventFormatBatchTest by testSuite("Protobuf event format — batch") {
    test("empty batch encodes to an empty message and decodes to an empty list") {
        val bytes = ProtobufEventFormat.encodeBatch(emptyList())
        assertContentEquals(ByteArray(0), bytes)
        assertEquals(emptyList(), ProtobufEventFormat.decodeBatch(bytes))
    }

    test("two events round-trip through CloudEventBatch") {
        val bytes = ProtobufEventFormat.encodeBatch(listOf(miniA, miniB))
        val decoded = ProtobufEventFormat.decodeBatch(bytes)
        assertEquals(listOf(miniA, miniB), decoded)
    }

    test("batch elements keep their own spec versions (mixed versions allowed)") {
        val decoded = ProtobufEventFormat.decodeBatch(ProtobufEventFormat.encodeBatch(listOf(miniA, miniB)))
        assertEquals(SpecVersion.V1_0, decoded[0].specVersion)
        assertEquals(SpecVersion.V0_3, decoded[1].specVersion)
    }

    test("batch is a stateless envelope over the single-event codec") {
        val mixed = listOf(miniA, miniB, miniA.copy { subject = "third" })
        val decoded = ProtobufEventFormat.decodeBatch(ProtobufEventFormat.encodeBatch(mixed))
        assertEquals(mixed, decoded)
    }

    test("batch carries data carriers faithfully") {
        val withText = miniA.copy { data = ProtobufEventData.Text("payload") }
        val withBinary = miniA.copy { data = ProtobufEventData.Binary(byteArrayOf(4, 5, 6)) }
        val decoded = ProtobufEventFormat.decodeBatch(ProtobufEventFormat.encodeBatch(listOf(withText, withBinary)))
        assertTrue(decoded[0].data is ProtobufEventData.Text)
        assertTrue(decoded[1].data is ProtobufEventData.Binary)
    }
}