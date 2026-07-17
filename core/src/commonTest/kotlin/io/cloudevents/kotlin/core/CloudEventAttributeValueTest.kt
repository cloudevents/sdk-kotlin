package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

val cloudEventAttributeValueTest by testSuite("Attribute type system") {
    test("Boolean round-trips through its canonical form") {
        assertEquals("true", BooleanValue(true).canonicalString)
        assertEquals("false", BooleanValue(false).canonicalString)
        assertEquals(BooleanValue(true), BooleanValue.fromCanonicalString("true"))
        assertEquals(BooleanValue(false), BooleanValue.fromCanonicalString("false"))
    }

    test("Boolean rejects a non-canonical string") {
        assertFailsWith<IllegalArgumentException> { BooleanValue.fromCanonicalString("TRUE") }
        assertFailsWith<IllegalArgumentException> { BooleanValue.fromCanonicalString("1") }
    }

    test("Integer round-trips and stays within signed 32-bit range") {
        assertEquals("42", IntegerValue(42).canonicalString)
        assertEquals(IntegerValue(-7), IntegerValue.fromCanonicalString("-7"))
        assertEquals(IntegerValue(Int.MAX_VALUE), IntegerValue.fromCanonicalString("2147483647"))
        assertFailsWith<NumberFormatException> { IntegerValue.fromCanonicalString("2147483648") }
    }

    test("String round-trips verbatim") {
        val text = "a value with spaces & symbols: /?#"
        assertEquals(text, StringValue(text).canonicalString)
        assertEquals(StringValue(text), StringValue.fromCanonicalString(text))
    }

    test("URI and URI-reference round-trip as their string form") {
        assertEquals("https://example.com/x", UriValue("https://example.com/x").canonicalString)
        assertEquals(UriValue("urn:x"), UriValue.fromCanonicalString("urn:x"))
        assertEquals("/sensors/1", UriReferenceValue("/sensors/1").canonicalString)
        assertEquals(UriReferenceValue("../a"), UriReferenceValue.fromCanonicalString("../a"))
    }

    test("Timestamp round-trips as RFC 3339") {
        val instant = Instant.parse("2024-06-15T14:30:00Z")
        assertEquals("2024-06-15T14:30:00Z", TimestampValue(instant).canonicalString)
        assertEquals(TimestampValue(instant), TimestampValue.fromCanonicalString("2024-06-15T14:30:00Z"))
    }

    test("Timestamp rejects a non-RFC-3339 string") {
        assertFailsWith<IllegalArgumentException> { TimestampValue.fromCanonicalString("15/06/2024") }
    }

    test("Binary round-trips through Base64") {
        val bytes = byteArrayOf(0, 1, 2, 3, 127, -1)
        val encoded = BinaryValue(bytes).canonicalString
        assertTrue(bytes.contentEquals(BinaryValue.fromCanonicalString(encoded).value))
    }

    test("Binary equality is content-based") {
        assertEquals(BinaryValue(byteArrayOf(1, 2, 3)), BinaryValue(byteArrayOf(1, 2, 3)))
        assertEquals(
            BinaryValue(byteArrayOf(1, 2, 3)).hashCode(),
            BinaryValue(byteArrayOf(1, 2, 3)).hashCode(),
        )
        assertNotEquals(BinaryValue(byteArrayOf(1, 2, 3)), BinaryValue(byteArrayOf(3, 2, 1)))
    }

    test("Binary defends against external mutation of its bytes") {
        val bytes = byteArrayOf(1, 2, 3)
        val binary = BinaryValue(bytes)
        bytes[0] = 9
        assertEquals(BinaryValue(byteArrayOf(1, 2, 3)), binary)
    }

    test("Binary defends against mutation of the bytes it returns") {
        val binary = BinaryValue(byteArrayOf(1, 2, 3))
        binary.value[0] = 9
        assertEquals(BinaryValue(byteArrayOf(1, 2, 3)), binary)
        assertEquals(BinaryValue(byteArrayOf(1, 2, 3)).hashCode(), binary.hashCode())
    }
}
