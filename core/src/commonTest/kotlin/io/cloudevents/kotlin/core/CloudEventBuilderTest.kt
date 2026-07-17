package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

val cloudEventBuilderTest by testSuite("Event construction") {
    val time = Instant.parse("2024-01-01T00:00:00Z")

    test("the DSL builds an event from assignable properties") {
        val event = cloudEvent("id-1", "/sensors/1", "com.example.created") {
            dataContentType = "application/json"
            subject = "s"
            this.time = time
            data = CloudEventData.wrap(byteArrayOf(1, 2, 3))
            extension("traceid", "abc")
            extension("retrycount", 2)
        }
        assertEquals("id-1", event.id)
        assertEquals(SpecVersion.V1_0, event.specVersion)
        assertEquals("application/json", event.dataContentType)
        assertEquals(StringValue("abc"), event.getExtension("traceid"))
        assertEquals(IntegerValue(2), event.getExtension("retrycount"))
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(event.data?.toBytes() ?: byteArrayOf()))
    }

    test("the fluent builder produces the same event as the DSL") {
        val fluent = CloudEventBuilder("id-1", "/sensors/1", "com.example.created")
            .extension("traceid", "abc")
            .build()
        val dsl = cloudEvent("id-1", "/sensors/1", "com.example.created") {
            extension("traceid", "abc")
        }
        assertEquals(dsl, fluent)
    }

    test("build rejects an illegally named extension") {
        assertFailsWith<IllegalArgumentException> {
            cloudEvent("1", "/s", "t") {
                extension("Bad-Name", "x")
            }
        }
    }

    test("copy derives a new immutable event, leaving the original unchanged") {
        val original = cloudEvent("id-1", "/s", "t") {
            subject = "before"
            extension("traceid", "abc")
        }
        val derived = original.copy {
            subject = "after"
            extension("retrycount", 5)
        }
        assertEquals("before", original.subject)
        assertNull(original.getExtension("retrycount"))
        assertEquals("after", derived.subject)
        assertEquals("id-1", derived.id)
        assertEquals(StringValue("abc"), derived.getExtension("traceid"))
        assertEquals(IntegerValue(5), derived.getExtension("retrycount"))
    }

    test("copy with no changes equals the original") {
        val original = cloudEvent("1", "/s", "t")
        assertEquals(original, original.copy())
    }

    test("removeExtension drops an extension during derivation") {
        val original = cloudEvent("1", "/s", "t") { extension("traceid", "abc") }
        val derived = original.copy { removeExtension("traceid") }
        assertNull(derived.getExtension("traceid"))
    }
}
