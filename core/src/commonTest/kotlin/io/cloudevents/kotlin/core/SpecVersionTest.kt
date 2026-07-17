package io.cloudevents.kotlin.core

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

val specVersionTest by testSuite("SpecVersion") {
    test("ofWireValue resolves the supported wire values") {
        assertEquals(SpecVersion.V1_0, SpecVersion.ofWireValue("1.0"))
        assertEquals(SpecVersion.V0_3, SpecVersion.ofWireValue("0.3"))
    }

    test("wireValue carries the on-the-wire specversion string") {
        assertEquals("1.0", SpecVersion.V1_0.wireValue)
        assertEquals("0.3", SpecVersion.V0_3.wireValue)
    }

    test("ofWireValue rejects an unsupported version") {
        assertFailsWith<IllegalArgumentException> { SpecVersion.ofWireValue("2.0") }
        assertFailsWith<IllegalArgumentException> { SpecVersion.ofWireValue("1.0.2") }
        assertFailsWith<IllegalArgumentException> { SpecVersion.ofWireValue("1.0.1") }
        assertFailsWith<IllegalArgumentException> { SpecVersion.ofWireValue("") }
    }
}
