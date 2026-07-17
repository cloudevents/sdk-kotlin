package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

val formatValidatorsTest by testSuite("Format validators") {
    test("RFC 3339 requires an offset and rejects malformed date-times") {
        assertTrue(Formats.isRfc3339("2024-06-15T14:30:00Z"))
        assertTrue(Formats.isRfc3339("2024-06-15T14:30:00+02:00"))
        assertFalse(Formats.isRfc3339("2024-06-15T14:30:00")) // no offset
        assertFalse(Formats.isRfc3339("15/06/2024"))
    }

    test("Integer validator enforces the signed 32-bit range") {
        assertTrue(Formats.isSignedInt32("2147483647"))
        assertTrue(Formats.isSignedInt32("-2147483648"))
        assertFalse(Formats.isSignedInt32("2147483648")) // overflow
        assertFalse(Formats.isSignedInt32("abc"))
    }

    test("URI-reference permits a relative reference; absolute URI requires a scheme") {
        assertTrue(Formats.isUriReference("/sensors/tn-1234567/alerts"))
        assertTrue(Formats.isUriReference("https://example.com/a?b#c"))
        assertTrue(Formats.isUriReference("ok%2Fencoded"))
        assertFalse(Formats.isUriReference("has space"))
        assertFalse(Formats.isUriReference("bad%zz"))

        assertTrue(Formats.isAbsoluteUri("https://example.com/schema"))
        assertTrue(Formats.isAbsoluteUri("urn:isbn:1234"))
        assertFalse(Formats.isAbsoluteUri("/sensors/1")) // relative, no scheme
    }

    test("URI validation enforces authority grammar, not just characters") {
        assertTrue(Formats.isAbsoluteUri("http://example.com:8080/path")) // numeric port
        assertTrue(Formats.isAbsoluteUri("http://user@example.com/path")) // userinfo
        assertTrue(Formats.isAbsoluteUri("http://[::1]/path")) // IPv6 literal
        assertTrue(Formats.isAbsoluteUri("http://[2001:db8::1]:8080/path")) // IPv6 literal with port
        assertFalse(Formats.isAbsoluteUri("http://[::1")) // unclosed IP-literal bracket
        assertFalse(Formats.isAbsoluteUri("http://example.com:abc")) // non-numeric port
        assertFalse(Formats.isAbsoluteUri("http://[::zz]")) // invalid IPv6 literal
    }

    test("String validator rejects control characters") {
        assertNull(Formats.firstStringViolation("normal text with symbols !?#"))
        assertNotNull(Formats.firstStringViolation("tab	here")) // C0 control
        assertNotNull(Formats.firstStringViolation("bell")) // C0 control
        assertNotNull(Formats.firstStringViolation("del")) // DEL
        assertNotNull(Formats.firstStringViolation("c1")) // C1 control
    }

    test("String validator rejects unpaired surrogates but accepts valid pairs") {
        // Build the lone surrogates at runtime rather than as string literals: a lone surrogate
        // cannot be encoded in UTF-8, so on Kotlin/JS it can be replaced with U+FFFD when the
        // compiled source is written to and read back from disk. Char(code) keeps it in memory.
        assertNotNull(Formats.firstStringViolation("lone" + Char(0xD800) + "high"))
        assertNotNull(Formats.firstStringViolation("lone" + Char(0xDC00) + "low"))
        assertNull(Formats.firstStringViolation("emoji 😀")) // U+1F600, a valid pair
    }

    test("String validator rejects Unicode noncharacters across planes") {
        assertNotNull(Formats.firstStringViolation("bmp￾")) // U+FFFE
        assertNotNull(Formats.firstStringViolation("bmp﷐")) // U+FDD0
        assertNotNull(Formats.firstStringViolation("supp🿾")) // U+1FFFE
    }
}

val formatValidationOnEventsTest by testSuite("Format validation on events") {
    fun event(block: CloudEventBuilder.() -> Unit) = cloudEvent("1", "/s", "t", block)

    test("an invalid source URI-reference is a violation") {
        val result = event {} .copy(source = "has space").validate()
        assertTrue(result.violations.any { it.attribute == "source" })
    }

    test("dataschema must be an absolute URI under v1.0") {
        assertFalse(event { dataSchema = "/relative" }.validate().isValid)
        assertTrue(event { dataSchema = "https://example.com/schema" }.validate().isValid)
    }

    test("a control character in a String attribute is a violation") {
        val result = event { subject = "badsubject" }.validate()
        assertTrue(result.violations.any { it.attribute == "subject" })
    }

    test("a control character in a String extension is a violation") {
        val result = event { extension("traceid", "badvalue") }.validate()
        assertTrue(result.violations.any { it.attribute == "traceid" })
    }
}
