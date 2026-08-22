// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import de.infix.testBalloon.framework.core.testSuite
import io.cloudevents.kotlin.core.BinaryValue
import io.cloudevents.kotlin.core.CloudEventBuilder
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.TimestampValue
import io.cloudevents.kotlin.core.cloudEvent
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

private fun JsonElement?.string(): String? = this?.jsonPrimitive?.content

private fun JsonElement?.num(): Int? = this?.jsonPrimitive?.content?.toInt()

val jsonEncodeTest by testSuite("JSON event format — encode") {
    test("a minimal v1.0 event encodes the required attributes and specversion") {
        val event = cloudEvent("A234-1234-1234", "/sensors/tn-1234567", "com.example.temperature")
        val doc = json.parseToJsonElement(JsonEventFormat.encodeToString(event)).jsonObject
        assertEquals("1.0", doc["specversion"].string())
        assertEquals("A234-1234-1234", doc["id"].string())
        assertEquals("/sensors/tn-1234567", doc["source"].string())
        assertEquals("com.example.temperature", doc["type"].string())
        // Unset optional attributes are omitted.
        assertTrue("datacontenttype" !in doc)
        assertTrue("subject" !in doc)
        assertTrue("time" !in doc)
    }

    test("context attributes map to their JSON types and unset optionals are omitted") {
        val event = cloudEvent("1", "/s", "t") {
            subject = "temperature"
            time = TimestampValue.fromCanonicalString("2024-06-15T14:30:00Z").value
            dataContentType = "application/json"
            dataSchema = "https://example.com/schema"
        }
        val doc = json.parseToJsonElement(JsonEventFormat.encodeToString(event)).jsonObject
        assertEquals("2024-06-15T14:30:00Z", doc["time"].string())
        assertEquals("application/json", doc["datacontenttype"].string())
        assertEquals("https://example.com/schema", doc["dataschema"].string())
        assertEquals("temperature", doc["subject"].string())
    }

    test("extensions become top-level members mapped through the type system") {
        val event = cloudEvent("1", "/s", "t") {
            extension("traceid", "abc-123")
            extension("retrycount", 3)
            extension("enabled", true)
        }
        val doc = json.parseToJsonElement(JsonEventFormat.encodeToString(event)).jsonObject
        assertEquals("abc-123", doc["traceid"].string())
        assertEquals(3, doc["retrycount"].num())
        assertEquals(true, doc["enabled"]?.jsonPrimitive?.booleanOrNull)
    }

    test("a Timestamp extension serializes as RFC 3339 and a Binary extension as Base64") {
        val event = cloudEvent("1", "/s", "t") {
            extension("stamp", TimestampValue.fromCanonicalString("2024-06-15T14:30:00Z"))
            extension("blob", BinaryValue(byteArrayOf(1, 2, 3)))
        }
        val doc = json.parseToJsonElement(JsonEventFormat.encodeToString(event)).jsonObject
        assertEquals("2024-06-15T14:30:00Z", doc["stamp"].string())
        assertEquals("AQID", doc["blob"].string())
    }

    test("a v0.3 event uses schemaurl and omits dataschema") {
        val event = cloudEvent("1", "/s", "t") {
            specVersion = SpecVersion.V0_3
            dataSchema = "/schemas/thing"
            dataContentEncoding = "base64"
        }
        val doc = json.parseToJsonElement(JsonEventFormat.encodeToString(event)).jsonObject
        assertEquals("0.3", doc["specversion"].string())
        assertEquals("/schemas/thing", doc["schemaurl"].string())
        assertEquals("base64", doc["datacontentencoding"].string())
        assertTrue("dataschema" !in doc)
    }

    test("JSON-declared data is embedded as a raw JSON value") {
        val event = CloudEventBuilder("1", "/s", "t")
            .withDataContentType("application/json")
            .withData("""{"appinfoA":"abc","appinfoB":123}""".encodeToByteArray())
            .build()
        val doc = json.parseToJsonElement(JsonEventFormat.encodeToString(event)).jsonObject
        assertEquals(123, doc["data"]?.jsonObject?.get("appinfoB").num())
        assertTrue("data_base64" !in doc)
    }

    test("v1.0 binary (non-JSON content type) data routes to data_base64") {
        val event = CloudEventBuilder("1", "/s", "t")
            .withDataContentType("application/vnd.apache.thrift.binary")
            .withData(byteArrayOf(1, 2, 3))
            .build()
        val doc = json.parseToJsonElement(JsonEventFormat.encodeToString(event)).jsonObject
        assertEquals("AQID", doc["data_base64"].string())
        assertTrue("data" !in doc)
    }

    test("v0.3 binary data routes to base64 under data with datacontentencoding") {
        val event = CloudEventBuilder("1", "/s", "t")
            .withSpecVersion(SpecVersion.V0_3)
            .withDataContentType("application/vnd.apache.thrift.binary")
            .withData(byteArrayOf(1, 2, 3))
            .build()
        val doc = json.parseToJsonElement(JsonEventFormat.encodeToString(event)).jsonObject
        assertEquals("AQID", doc["data"].string())
        assertEquals("base64", doc["datacontentencoding"].string())
        assertTrue("data_base64" !in doc)
    }

    test("payload declared JSON but not a JSON document fails loudly") {
        val event = CloudEventBuilder("1", "/s", "t")
            .withDataContentType("application/json")
            .withData("not json".encodeToByteArray())
            .build()
        assertFailsWith<JsonEventFormatException> {
            JsonEventFormat.encodeToString(event)
        }
    }

    test("an explicit-null data round-trips as a JSON null rather than an absent member") {
        val event = CloudEventBuilder("1", "/s", "t").withData(NullData).build()
        val doc = json.parseToJsonElement(JsonEventFormat.encodeToString(event)).jsonObject
        assertEquals("null", doc["data"].string())
        assertTrue("data_base64" !in doc)
    }

    test("absent data encodes to no data member") {
        val event = cloudEvent("1", "/s", "t")
        val doc = json.parseToJsonElement(JsonEventFormat.encodeToString(event)).jsonObject
        assertTrue("data" !in doc)
        assertTrue("data_base64" !in doc)
    }
}
