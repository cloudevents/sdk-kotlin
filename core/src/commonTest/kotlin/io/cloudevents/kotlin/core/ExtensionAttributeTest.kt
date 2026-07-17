package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

val extensionAttributeTest by testSuite("Extension attributes") {
    fun event(extensions: Map<String, CloudEventAttributeValue>) = CloudEvent(
        id = "id-1",
        source = "/s",
        type = "t",
        extensions = extensions,
    )

    test("getExtension returns the typed value, using the shared type system") {
        val e = event(
            mapOf(
                "traceid" to StringValue("abc"),
                "retrycount" to IntegerValue(3),
                "sampled" to BooleanValue(true),
            ),
        )
        assertEquals(StringValue("abc"), e.getExtension("traceid"))
        assertEquals(IntegerValue(3), e.getExtension("retrycount"))
        assertEquals(BooleanValue(true), e.getExtension("sampled"))
    }

    test("getExtension returns null for an absent extension") {
        assertNull(event(emptyMap()).getExtension("missing"))
    }

    test("extensionNames lists the present extensions") {
        assertEquals(setOf("traceid", "retrycount"), event(mapOf("traceid" to StringValue("a"), "retrycount" to IntegerValue(1))).extensionNames)
    }

    test("getExtensionAs narrows to a value type or yields null on a mismatch") {
        val e = event(mapOf("retrycount" to IntegerValue(3)))
        assertEquals(3, e.getExtensionAs<IntegerValue>("retrycount")?.value)
        assertNull(e.getExtensionAs<StringValue>("retrycount"))
        assertNull(e.getExtensionAs<IntegerValue>("missing"))
    }

    test("extension names obey the same MUST naming rules as core attributes") {
        assertFailsWith<IllegalArgumentException> { event(mapOf("data" to StringValue("x"))) }
        assertFailsWith<IllegalArgumentException> { event(mapOf("myExt" to StringValue("x"))) }
        assertFailsWith<IllegalArgumentException> { event(mapOf("my-ext" to StringValue("x"))) }
    }

    test("extensions participate in equality and are held immutably") {
        assertEquals(event(mapOf("a" to IntegerValue(1))), event(mapOf("a" to IntegerValue(1))))

        val mutable = mutableMapOf<String, CloudEventAttributeValue>("a" to IntegerValue(1))
        val e = event(mutable)
        mutable["a"] = IntegerValue(99)
        assertEquals(IntegerValue(1), e.getExtension("a"))
    }
}
