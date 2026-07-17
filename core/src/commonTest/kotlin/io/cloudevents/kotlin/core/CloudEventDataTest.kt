package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

val cloudEventDataTest by testSuite("CloudEventData seam") {
    test("wrap exposes the payload bytes") {
        val data = CloudEventData.wrap(byteArrayOf(1, 2, 3))
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(data.toBytes()))
    }

    test("equality is content-based over the bytes") {
        assertEquals(CloudEventData.wrap(byteArrayOf(1, 2, 3)), CloudEventData.wrap(byteArrayOf(1, 2, 3)))
        assertEquals(
            CloudEventData.wrap(byteArrayOf(1, 2, 3)).hashCode(),
            CloudEventData.wrap(byteArrayOf(1, 2, 3)).hashCode(),
        )
        assertNotEquals(CloudEventData.wrap(byteArrayOf(1, 2, 3)), CloudEventData.wrap(byteArrayOf(9)))
    }

    test("wrap and toBytes defend against external mutation") {
        val bytes = byteArrayOf(1, 2, 3)
        val data = CloudEventData.wrap(bytes)
        bytes[0] = 9
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(data.toBytes()))

        val exported = data.toBytes()
        exported[0] = 9
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(data.toBytes()))
    }

    test("CloudEvent carries an optional data payload participating in equality") {
        fun event(data: CloudEventData?) = CloudEvent(
            id = "id-1",
            source = "/s",
            type = "t",
            data = data,
        )
        assertNull(event(null).data)
        assertEquals(event(CloudEventData.wrap(byteArrayOf(1))), event(CloudEventData.wrap(byteArrayOf(1))))
        assertNotEquals(event(CloudEventData.wrap(byteArrayOf(1))), event(null))
        assertNotEquals(
            event(CloudEventData.wrap(byteArrayOf(1))),
            event(CloudEventData.wrap(byteArrayOf(2))),
        )
    }
}
