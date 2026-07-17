package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val validationModesTest by testSuite("Validation modes") {
    val invalid = cloudEvent("", "", "")
    val valid = cloudEvent("1", "/s", "t")

    test("strict is the default and throws on an invalid event") {
        assertFailsWith<CloudEventValidationException> { invalid.validate() }
        assertFailsWith<CloudEventValidationException> { invalid.validate(ValidationMode.STRICT) }
    }

    test("strict returns the result for a valid event without throwing") {
        val result = valid.validate()
        assertTrue(result.isValid)
    }

    test("lenient never throws and returns every collected violation") {
        val result = invalid.validate(ValidationMode.LENIENT)
        assertFalse(result.isValid)
        assertEquals(setOf("id", "source", "type"), result.violations.map { it.attribute }.toSet())
    }

    test("the strict exception carries the full validation result") {
        val exception = assertFailsWith<CloudEventValidationException> { invalid.validate() }
        assertEquals(setOf("id", "source", "type"), exception.result.violations.map { it.attribute }.toSet())
    }
}
