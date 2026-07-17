package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

val attributeNamingTest by testSuite("Attribute naming rules") {
    test("accepts lowercase-alphanumeric names") {
        assertEquals("myext", AttributeNaming.requireValidName("myext"))
        assertEquals("ext123", AttributeNaming.requireValidName("ext123"))
        assertEquals("123", AttributeNaming.requireValidName("123"))
    }

    test("rejects the reserved name data") {
        assertFailsWith<IllegalArgumentException> { AttributeNaming.requireValidName("data") }
    }

    test("rejects names with characters outside a-z and 0-9") {
        assertFailsWith<IllegalArgumentException> { AttributeNaming.requireValidName("myExt") }
        assertFailsWith<IllegalArgumentException> { AttributeNaming.requireValidName("my-ext") }
        assertFailsWith<IllegalArgumentException> { AttributeNaming.requireValidName("my_ext") }
        assertFailsWith<IllegalArgumentException> { AttributeNaming.requireValidName("my.ext") }
        assertFailsWith<IllegalArgumentException> { AttributeNaming.requireValidName("") }
    }

    test("does not block SHOULD-level names at construction") {
        // 21 characters — exceeds the SHOULD limit of 20 but is not a MUST violation.
        val longName = "a".repeat(21)
        assertEquals(longName, AttributeNaming.requireValidName(longName))
        // Leading digit — SHOULD start with a letter, but that is not enforced at construction.
        assertEquals("1abc", AttributeNaming.requireValidName("1abc"))
    }
}
