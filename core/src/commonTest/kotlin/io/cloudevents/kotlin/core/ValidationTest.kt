package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val validationTest by testSuite("Per-event validation") {
    test("a well-formed v1.0.2 event is valid") {
        val event = cloudEvent("1", "/s", "t")
        val result = event.validate()
        assertTrue(result.isValid)
        assertTrue(result.violations.isEmpty())
    }

    test("a missing (empty) REQUIRED attribute is a violation") {
        val event = cloudEvent("", "/s", "t")
        val result = event.validate(ValidationMode.LENIENT)
        assertFalse(result.isValid)
        assertEquals(listOf("id"), result.violations.map { it.attribute })
    }

    test("all violations are reported rather than stopping at the first") {
        val event = cloudEvent("", "", "")
        val result = event.validate(ValidationMode.LENIENT)
        assertEquals(setOf("id", "source", "type"), result.violations.map { it.attribute }.toSet())
    }

    test("a present but empty subject is a violation") {
        val event = cloudEvent("1", "/s", "t") { subject = "" }
        val result = event.validate(ValidationMode.LENIENT)
        assertFalse(result.isValid)
        assertTrue(result.violations.any { it.attribute == "subject" })
    }

    test("validation branches on specversion and accepts a well-formed v0.3 event") {
        val event = cloudEvent("1", "/s", "t") {
            specVersion = SpecVersion.V0_3
        }
        assertTrue(event.validate().isValid)
    }
}
