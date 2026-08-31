// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

import de.infix.testBalloon.framework.core.testSuite
import io.cloudevents.kotlin.core.BooleanValue
import kotlin.test.assertEquals

val protobufModuleSmoke by testSuite("Protobuf module smoke") {
    test("Protobuf media type constants are exposed") {
        assertEquals("application/cloudevents+protobuf", ProtobufEventFormat.mediaType)
        assertEquals("application/cloudevents-batch+protobuf", ProtobufEventFormat.batchMediaType)
    }
    test("Protobuf module sees a core type") {
        assertEquals("true", BooleanValue(true).canonicalString)
    }
}