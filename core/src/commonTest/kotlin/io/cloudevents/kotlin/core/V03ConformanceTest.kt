package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val specVersionVocabularyTest by testSuite("SpecVersion attribute vocabulary") {
    test("v1.0 defines dataschema and omits the v0.3-only attributes") {
        assertTrue("dataschema" in SpecVersion.V1_0.optionalAttributes)
        assertFalse("schemaurl" in SpecVersion.V1_0.allAttributes)
        assertFalse("datacontentencoding" in SpecVersion.V1_0.allAttributes)
    }

    test("v0.3 defines schemaurl and datacontentencoding in place of dataschema") {
        assertTrue("schemaurl" in SpecVersion.V0_3.optionalAttributes)
        assertTrue("datacontentencoding" in SpecVersion.V0_3.optionalAttributes)
        assertFalse("dataschema" in SpecVersion.V0_3.allAttributes)
    }
}

val v03ConformanceTest by testSuite("v0.3 conformance") {
    fun v03(block: CloudEventBuilder.() -> Unit) =
        cloudEvent("1", "/source", "t") { specVersion = SpecVersion.V0_3; block() }

    test("a well-formed v0.3 event with schemaurl and datacontentencoding is valid") {
        val event = v03 {
            dataSchema = "/schemas/thing" // schemaurl: a URI-reference, relative permitted
            dataContentEncoding = "base64"
        }
        assertTrue(event.validate(ValidationMode.LENIENT).isValid)
    }

    test("schemaurl that is not a URI-reference is a violation") {
        val result = v03 { dataSchema = "has space" }.validate(ValidationMode.LENIENT)
        assertTrue(result.violations.any { it.attribute == "schemaurl" })
    }

    test("an empty datacontentencoding is a violation") {
        val result = v03 { dataContentEncoding = "" }.validate(ValidationMode.LENIENT)
        assertTrue(result.violations.any { it.attribute == "datacontentencoding" })
    }

    test("a datacontentencoding that is not an RFC 2045 token is a violation") {
        val result = v03 { dataContentEncoding = "not an encoding" }.validate(ValidationMode.LENIENT)
        assertTrue(result.violations.any { it.attribute == "datacontentencoding" })
    }

    test("an extension name not beginning with a lowercase letter is a violation") {
        val result = v03 { extension("1trace", "x") }.validate(ValidationMode.LENIENT)
        assertTrue(result.violations.any { it.attribute == "1trace" })
    }

    test("the Boolean and absolute-URI types are rejected as v0.3 attribute values") {
        val result = v03 {
            extension("flag", true) // BooleanValue
            extension("schema", UriValue("https://example.com/x")) // absolute URI type
            extension("ref", UriReferenceValue("/relative")) // permitted in v0.3
        }.validate(ValidationMode.LENIENT)
        assertTrue(result.violations.any { it.attribute == "flag" })
        assertTrue(result.violations.any { it.attribute == "schema" })
        assertFalse(result.violations.any { it.attribute == "ref" })
    }
}

val v10VersusV03Test by testSuite("v1.0 versus v0.3 attribute gating") {
    test("datacontentencoding is rejected under v1.0") {
        val event = cloudEvent("1", "/s", "t") { dataContentEncoding = "base64" }
        val result = event.validate(ValidationMode.LENIENT)
        assertTrue(result.violations.any { it.attribute == "datacontentencoding" })
    }

    test("dataschema must be absolute under v1.0 but a schemaurl may be relative under v0.3") {
        val relative = "/schemas/thing"
        assertFalse(
            cloudEvent("1", "/s", "t") { dataSchema = relative }.validate(ValidationMode.LENIENT).isValid,
        )
        assertTrue(
            cloudEvent("1", "/s", "t") {
                specVersion = SpecVersion.V0_3
                dataSchema = relative
            }.validate(ValidationMode.LENIENT).isValid,
        )
    }

    test("copy preserves datacontentencoding") {
        val event = cloudEvent("1", "/s", "t") { specVersion = SpecVersion.V0_3; dataContentEncoding = "base64" }
        assertEquals("base64", event.copy(id = "2").dataContentEncoding)
    }
}
