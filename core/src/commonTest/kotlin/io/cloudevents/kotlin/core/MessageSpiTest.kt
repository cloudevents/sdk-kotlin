package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A test-only representation a writer builds, standing in for a serialized message. Proves the SPI
 * shape is usable end to end; core itself ships no implementation.
 */
private data class Record(
    val specVersion: SpecVersion,
    val attributes: Map<String, CloudEventAttributeValue>,
    val data: CloudEventData?,
)

private class RecordWriter(val specVersion: SpecVersion) : CloudEventWriter<Record> {
    val attributes = mutableMapOf<String, CloudEventAttributeValue>()

    override fun withContextAttribute(name: String, value: CloudEventAttributeValue): CloudEventWriter<Record> {
        attributes[name] = value
        return this
    }

    override fun end(data: CloudEventData?): Record = Record(specVersion, attributes.toMap(), data)
}

/** A reader over a fixed in-memory message, driving whatever writer the factory produces. */
private class FixedMessageReader(
    val specVersion: SpecVersion,
    val attributes: Map<String, CloudEventAttributeValue>,
    val data: CloudEventData?,
) : MessageReader {
    override fun <R> read(writerFactory: CloudEventWriterFactory<R>): R {
        var writer = writerFactory.create(specVersion)
        for ((name, value) in attributes) {
            writer = writer.withContextAttribute(name, value)
        }
        return writer.end(data)
    }
}

val messageSpiTest by testSuite("Message SPI") {
    test("a reader drives a writer to reproduce attributes and payload") {
        val attributes = mapOf(
            "id" to StringValue("id-1"),
            "source" to UriReferenceValue("/s"),
            "type" to StringValue("t"),
            "myextension" to IntegerValue(7),
        )
        val reader = FixedMessageReader(SpecVersion.V1_0, attributes, CloudEventData.wrap(byteArrayOf(1, 2)))

        val record = reader.read { specVersion -> RecordWriter(specVersion) }

        assertEquals(SpecVersion.V1_0, record.specVersion)
        assertEquals(attributes, record.attributes)
        assertTrue(byteArrayOf(1, 2).contentEquals(record.data?.toBytes() ?: byteArrayOf()))
    }

    test("the writer factory receives the wire specversion the reader observed") {
        val reader = FixedMessageReader(SpecVersion.V0_3, emptyMap(), null)
        val record = reader.read { specVersion -> RecordWriter(specVersion) }
        assertEquals(SpecVersion.V0_3, record.specVersion)
        assertNull(record.data)
    }
}
