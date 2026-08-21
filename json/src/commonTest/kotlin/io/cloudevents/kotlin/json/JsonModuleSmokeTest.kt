// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import de.infix.testBalloon.framework.core.testSuite
import io.cloudevents.kotlin.core.BooleanValue
import kotlin.test.assertEquals

val jsonModuleSmoke by testSuite("JSON module smoke") {
    test("JSON media type constant is exposed") {
        assertEquals("application/cloudevents+json", JsonEventFormat.mediaType)
    }
    test("JSON module sees a core type") {
        assertEquals("true", BooleanValue(true).canonicalString)
    }
}
