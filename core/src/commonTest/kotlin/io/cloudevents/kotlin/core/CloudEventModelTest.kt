package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Instant

val cloudEventModelTest by testSuite("CloudEvent model") {
    val time = Instant.parse("2024-01-01T00:00:00Z")

    fun event(
        id: String = "id-1",
        source: String = "/sensors/1",
        type: String = "com.example.created",
        subject: String? = "s",
    ) = CloudEvent(
        id = id,
        source = source,
        type = type,
        specVersion = SpecVersion.V1_0,
        dataContentType = "application/json",
        dataSchema = "https://example.com/schema",
        subject = subject,
        time = time,
    )

    test("exposes the four REQUIRED context attributes") {
        val e = event()
        assertEquals("id-1", e.id)
        assertEquals("/sensors/1", e.source)
        assertEquals("com.example.created", e.type)
        assertEquals(SpecVersion.V1_0, e.specVersion)
    }

    test("exposes the four OPTIONAL context attributes") {
        val e = event()
        assertEquals("application/json", e.dataContentType)
        assertEquals("https://example.com/schema", e.dataSchema)
        assertEquals("s", e.subject)
        assertEquals(time, e.time)
    }

    test("permits absent OPTIONAL attributes") {
        val e = CloudEvent(
            id = "id-1",
            source = "/sensors/1",
            type = "com.example.created",
            specVersion = SpecVersion.V1_0,
            dataContentType = null,
            dataSchema = null,
            subject = null,
            time = null,
        )
        assertNull(e.dataContentType)
        assertNull(e.dataSchema)
        assertNull(e.subject)
        assertNull(e.time)
    }

    test("equals and hashCode are value-based over all attributes") {
        assertEquals(event(), event())
        assertEquals(event().hashCode(), event().hashCode())
    }

    test("differs when any attribute differs") {
        assertNotEquals(event(), event(id = "id-2"))
        assertNotEquals(event(subject = "a"), event(subject = null))
    }
}
