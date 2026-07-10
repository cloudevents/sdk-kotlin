// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val smokeTest by testSuite("Platform smoke") {
    test("basic arithmetic works on this platform") {
        assertEquals(4, 2 + 2)
    }
    test("package name is correct") {
        assertTrue("io.cloudevents.kotlin.core".startsWith("io.cloudevents"))
    }
}
