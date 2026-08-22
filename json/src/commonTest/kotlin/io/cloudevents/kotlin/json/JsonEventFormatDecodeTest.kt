// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import de.infix.testBalloon.framework.core.testSuite
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.TimestampValue
import kotlin.io.encoding.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

val jsonDecodeTest by testSuite("JSON event format — decode") {
    test("decodes a minimal v1.0 document") {
        val event = JsonEventFormat.decodeFromString("""{"specversion":"1.0","id":"A234-1234-1234","source":"/sensors/tn-1234567","type":"com.example.temperature"}""")
        assertEquals("1.0", event.specVersion.wireValue)
        assertEquals("A234-1234-1234", event.id)
        assertEquals("/sensors/tn-1234567", event.source)
        assertEquals("com.example.temperature", event.type)
    }

    test("decodes v1.0 optional attributes and the data member routing") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"1.0","id":"1","source":"/s","type":"t","subject":"temperature","time":"2024-06-15T14:30:00Z","datacontenttype":"application/json","dataschema":"https://example.com/schema","data":{"appinfoA":"abc"}}""",
        )
        assertEquals("temperature", event.subject)
        assertEquals(TimestampValue.fromCanonicalString("2024-06-15T14:30:00Z").value, event.time)
        assertEquals("application/json", event.dataContentType)
        assertEquals("https://example.com/schema", event.dataSchema)
        assertIs<JsonData>(event.data)
    }

    test("extension members are preserved as an open key set") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"1.0","id":"1","source":"/s","type":"t","traceid":"abc","retrycount":3,"enabled":true}""",
        )
        assertEquals("abc", event.getExtension("traceid")?.canonicalString)
        assertEquals("3", event.getExtension("retrycount")?.canonicalString)
        assertEquals("true", event.getExtension("enabled")?.canonicalString)
    }

    test("a v0.3 document is bootstrapped from specversion and uses schemaurl") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"0.3","id":"1","source":"/s","type":"t","schemaurl":"/schemas/thing","datacontentencoding":"base64","data":"AQID"}""",
        )
        assertEquals(SpecVersion.V0_3, event.specVersion)
        assertEquals("/schemas/thing", event.dataSchema)
        assertEquals("base64", event.dataContentEncoding)
        // v0.3 base64 data decodes to a binary carrier.
        assertIs<Base64Data>(event.data)
    }

    test("v1.0 data_base64 decodes to a binary carrier") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"1.0","id":"1","source":"/s","type":"t","datacontenttype":"application/vnd.apache.thrift.binary","data_base64":"AQID"}""",
        )
        assertIs<Base64Data>(event.data)
        assertEquals("AQID", Base64.encode(event.data!!.toBytes()))
    }

    test("data and data_base64 together are rejected (contradictory §3.1.1 payload)") {
        assertFailsWith<JsonEventFormatException> {
            JsonEventFormat.decodeFromString(
                """{"specversion":"1.0","id":"1","source":"/s","type":"t","data":"x","data_base64":"eA=="}""",
            )
        }
    }

    test("an unknown specversion produces a clear error") {
        assertFailsWith<JsonEventFormatException> {
            JsonEventFormat.decodeFromString(
                """{"specversion":"9.9","id":"1","source":"/s","type":"t"}""",
            )
        }
    }

    test("a non-object document is rejected for structured mode") {
        assertFailsWith<JsonEventFormatException> {
            JsonEventFormat.decodeFromString("[1,2,3]")
        }
    }

    test("a missing required attribute is rejected") {
        assertFailsWith<JsonEventFormatException> {
            JsonEventFormat.decodeFromString("""{"specversion":"1.0","source":"/s","type":"t"}""")
        }
    }

    test("an invalid event is validated rather than silently trusted") {
        // subject with a control character is a String-type violation.
        assertFailsWith<JsonEventFormatException> {
            JsonEventFormat.decodeFromString("""{"specversion":"1.0","id":"1","source":"/s","type":"t","subject":"bad\u0001subject"}""")
        }
    }

    test("absent data decodes to a null payload and null data stays explicit") {
        val absent = JsonEventFormat.decodeFromString("""{"specversion":"1.0","id":"1","source":"/s","type":"t"}""")
        assertNull(absent.data)

        val explicitNull = JsonEventFormat.decodeFromString("""{"specversion":"1.0","id":"1","source":"/s","type":"t","data":null}""")
        assertIs<NullData>(explicitNull.data)
    }

    test("round-trip decode(encode(e)) is semantically stable for a v1.0 event") {
        val original = io.cloudevents.kotlin.core.cloudEvent("1", "/s", "t") {
            subject = "temperature"
            time = TimestampValue.fromCanonicalString("2024-06-15T14:30:00Z").value
            extension("traceid", "abc-123")
            extension("retrycount", 7)
        }
        val decoded = JsonEventFormat.decodeFromString(JsonEventFormat.encodeToString(original))
        assertEquals(original, decoded)
    }
}
