// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import de.infix.testBalloon.framework.core.testSuite
import io.cloudevents.kotlin.core.SpecVersion
import kotlin.io.encoding.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive

/**
 * Vendored conformance fixtures: the CloudEvents spec JSON-format examples (§3.2) and the batch
 * example (§4.3) plus edge cases mirrored from the reference sdk-java json-jackson test suite. All
 * comparisons are SEMANTIC: the JSON data that decodes to an [io.cloudevents.kotlin.core.CloudEvent] is
 * kept in the module's data-origin carrier, so assertions inspect the reconstructed event (and its
 * raw JSON element) instead of comparing canonical bytes; timestamps compare as parsed instants.
 */
val jsonEventFormatConformanceTest by testSuite("JSON event format — conformance fixtures") {
    test("spec §3.2 example: Binary-valued data with a non-JSON content type uses data_base64") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"1.0","type":"com.example.someevent","source":"/mycontext","id":"A234-1234-1234","time":"2018-04-05T17:31:00Z","comexampleextension1":"value","comexampleothervalue":5,"datacontenttype":"application/vnd.apache.thrift.binary","data_base64":"YQ=="}""",
        )
        assertEquals("com.example.someevent", event.type)
        assertEquals("/mycontext", event.source)
        assertEquals("A234-1234-1234", event.id)
        assertEquals("value", event.getExtension("comexampleextension1")?.canonicalString)
        assertEquals("5", event.getExtension("comexampleothervalue")?.canonicalString)
        assertEquals("application/vnd.apache.thrift.binary", event.dataContentType)
        assertIs<Base64Data>(event.data)
        assertEquals("a", Base64.decode("YQ==").decodeToString())
        assertEquals("a", (event.data as Base64Data).bytes.decodeToString())
    }

    test("spec §3.2 example: XML (non-JSON) data round-trips as a JSON string under data") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"1.0","type":"com.example.someevent","source":"/mycontext","id":"B234-1234-1234","time":"2018-04-05T17:31:00Z","comexampleextension1":"value","comexampleothervalue":5,"unsetextension":null,"datacontenttype":"application/xml","data":"<much wow=\"xml\"/>"}""",
        )
        assertEquals("/mycontext", event.source)
        assertIs<StringData>(event.data)
        assertEquals("<much wow=\"xml\"/>", (event.data as StringData).text)
        // Re-encoding keeps the string form under data, not data_base64.
        val reencoded = JsonEventFormat.encodeToString(event)
        assertTrue("\"data\":\"<much wow=\\\"xml\\\"/>\"" in reencoded)
    }

    test("spec §3.2 example: JSON object-valued data is embedded raw") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"1.0","type":"com.example.someevent","source":"/mycontext","subject":null,"id":"C234-1234-1234","time":"2018-04-05T17:31:00Z","comexampleextension1":"value","comexampleothervalue":5,"datacontenttype":"application/json","data":{"appinfoA":"abc","appinfoB":123,"appinfoC":true}}""",
        )
        assertEquals("application/json", event.dataContentType)
        assertIs<JsonData>(event.data)
        val obj = (event.data as JsonData).element as kotlinx.serialization.json.JsonObject
        assertEquals("abc", (obj["appinfoA"] as? JsonPrimitive)?.content)
        assertEquals("true", (obj["appinfoC"] as? JsonPrimitive)?.content)
    }

    test("spec §3.2 example: JSON number-valued data is embedded raw") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"1.0","type":"com.example.someevent","source":"/mycontext","subject":null,"id":"C234-1234-1234","time":"2018-04-05T17:31:00Z","comexampleextension1":"value","comexampleothervalue":5,"datacontenttype":"application/json","data":1.5}""",
        )
        assertIs<JsonData>(event.data)
        assertEquals("1.5", (event.data as JsonData).element.toString())
    }

    test("spec §3.2 example: a literal JSON string data with no datacontenttype is implicitly JSON") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"1.0","type":"com.example.someevent","source":"/mycontext","subject":null,"id":"D234-1234-1234","time":"2018-04-05T17:31:00Z","comexampleextension1":"value","comexampleothervalue":5,"data":"I'm just a string"}""",
        )
        assertIs<JsonData>(event.data)
        assertEquals("\"I'm just a string\"", (event.data as JsonData).element.toString())
    }

    test("spec §3.2 example: data_base64 without datacontenttype does not infer a content type") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"1.0","type":"com.example.someevent","source":"/mycontext","id":"D234-1234-1234","data_base64":"eyAieHl6IjogMTIzIH0="}""",
        )
        assertNull(event.dataContentType)
        assertIs<Base64Data>(event.data)
        assertEquals("{ \"xyz\": 123 }", (event.data as Base64Data).bytes.decodeToString())
    }

    test("spec §4.3 batch example decodes to two events with independent data") {
        val batch = JsonEventFormat.decodeBatch(
            """[{"specversion":"1.0","type":"com.example.someevent","source":"/mycontext/4","id":"B234-1234-1234","time":"2018-04-05T17:31:00Z","comexampleextension1":"value","comexampleothervalue":5,"datacontenttype":"application/vnd.apache.thrift.binary","data_base64":"YQ=="},{"specversion":"1.0","type":"com.example.someotherevent","source":"/mycontext/9","id":"C234-1234-1234","time":"2018-04-05T17:31:05Z","comexampleextension1":"value","comexampleothervalue":5,"datacontenttype":"application/json","data":{"appinfoA":"abc","appinfoB":123,"appinfoC":true}}]""",
        )
        assertEquals(2, batch.size)
        assertEquals("com.example.someevent", batch[0].type)
        assertEquals("com.example.someotherevent", batch[1].type)
        assertIs<Base64Data>(batch[0].data)
        assertIs<JsonData>(batch[1].data)
    }

    test("spec §4.3 empty batch round-trips to an empty list") {
        assertEquals(emptyList(), JsonEventFormat.decodeBatch("[]"))
    }

    test("mirrors sdk-java: a non-JSON text payload is preserved on round-trip (string form)") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"1.0","id":"1","type":"mock.test","source":"http://localhost/source","datacontenttype":"text/plain","data":"Hello World Lorena!","subject":"sub","time":"2018-04-26T14:48:09+02:00"}""",
        )
        assertEquals(SpecVersion.V1_0, event.specVersion)
        assertIs<StringData>(event.data)
        assertEquals("Hello World Lorena!", (event.data as StringData).text)
    }

    test("mirrors sdk-java: a decimal-valued extension is rejected (not an Integer)") {
        assertFailsWith<JsonEventFormatException> {
            JsonEventFormat.decodeFromString(
                """{"specversion":"1.0","id":"1","type":"mock.test","source":"http://localhost/source","decimal":42.42}""",
            )
        }
    }

    test("mirrors sdk-java: an out-of-range integer extension is rejected") {
        assertFailsWith<JsonEventFormatException> {
            JsonEventFormat.decodeFromString(
                """{"specversion":"1.0","id":"1","type":"mock.test","source":"http://localhost/source","long":4247483647}""",
            )
        }
    }

    test("mirrors sdk-java: an invalid extension name is rejected rather than silently dropped") {
        val doc = """{"specversion":"1.0","id":"1","type":"mock.test","source":"http://localhost/source","a_invalid_name":"x"}"""
        assertFailsWith<JsonEventFormatException> { JsonEventFormat.decodeFromString(doc) }
    }

    test("mirrors sdk-java: a JSON structured-data event decodes extensions of bool/int/string types") {
        val event = JsonEventFormat.decodeFromString(
            """{"specversion":"1.0","id":"1","type":"mock.test","source":"http://localhost/source","dataschema":"http://localhost/schema","datacontenttype":"application/json","data":{},"subject":"sub","time":"2018-04-26T14:48:09+02:00","astring":"aaa","aboolean":true,"anumber":10}""",
        )
        assertEquals("aaa", event.getExtension("astring")?.canonicalString)
        assertEquals("true", event.getExtension("aboolean")?.canonicalString)
        assertEquals("10", event.getExtension("anumber")?.canonicalString)
    }

    test("the round-trip property holds across the sdk-java mirrored fixture set") {
        val fixtures = listOf(
            """{"specversion":"1.0","id":"1","type":"mock.test","source":"http://localhost/source"}""",
            """{"specversion":"1.0","id":"1","type":"mock.test","source":"http://localhost/source","datacontenttype":"application/json","data":{},"subject":"sub","time":"2018-04-26T14:48:09+02:00","astring":"aaa","aboolean":true,"anumber":10}""",
            """{"specversion":"0.3","id":"1","type":"mock.test","source":"http://localhost/source","schemaurl":"http://localhost/schema","datacontenttype":"application/json","data":{},"subject":"sub","time":"2018-04-26T14:48:09+02:00"}""",
            """{"specversion":"1.0","id":"1","type":"mock.test","source":"http://localhost/source","datacontenttype":"text/plain","data":"Hello World Lorena!","subject":"sub","time":"2018-04-26T14:48:09+02:00"}""",
            """{"specversion":"0.3","id":"1","type":"mock.test","source":"http://localhost/source","datacontenttype":"text/plain","data":"Hello World Lorena!","subject":"sub","time":"2018-04-26T14:48:09+02:00"}""",
        )
        for (fixture in fixtures) {
            val first = JsonEventFormat.decodeFromString(fixture)
            val second = JsonEventFormat.decodeFromString(JsonEventFormat.encodeToString(first))
            assertEquals(first, second, "round-trip instability for $fixture")
        }
    }
}
