// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import de.infix.testBalloon.framework.core.testSuite
import io.cloudevents.kotlin.core.SpecVersion
import io.cloudevents.kotlin.core.cloudEvent
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

val jsonBatchTest by testSuite("JSON event format — batch") {
    test("an empty batch round-trips to an empty list") {
        val encoded = JsonEventFormat.encodeBatch(emptyList())
        assertEquals("[]", encoded)
        assertEquals(emptyList(), JsonEventFormat.decodeBatch(encoded))
    }

    test("a batch with mixed versions round-trips element-wise") {
        val v1 = cloudEvent("1", "/s", "t") { subject = "v1" }
        val v03 = cloudEvent("1", "/s", "t") { specVersion = SpecVersion.V0_3; subject = "v03" }
        val encoded = JsonEventFormat.encodeBatch(listOf(v1, v03))
        val decoded = JsonEventFormat.decodeBatch(encoded)
        assertEquals(2, decoded.size)
        assertEquals(SpecVersion.V1_0, decoded[0].specVersion)
        assertEquals(SpecVersion.V0_3, decoded[1].specVersion)
        assertEquals(listOf(v1, v03), decoded)
    }

    test("the batch document is a JSON array") {
        val encoded = JsonEventFormat.encodeBatch(listOf(cloudEvent("1", "/s", "t")))
        assertEquals(1, Json.parseToJsonElement(encoded).jsonArray.size)
    }

    test("a non-array batch document is rejected") {
        assertFailsWith<JsonEventFormatException> {
            JsonEventFormat.decodeBatch("""{"specversion":"1.0"}""")
        }
    }

    test("an array with a non-object element is rejected") {
        assertFailsWith<JsonEventFormatException> {
            JsonEventFormat.decodeBatch("""[{"specversion":"1.0","id":"1","source":"/s","type":"t"},42]""")
        }
    }
}
