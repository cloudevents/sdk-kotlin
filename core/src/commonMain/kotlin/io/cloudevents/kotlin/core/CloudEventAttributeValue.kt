// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

import kotlin.io.encoding.Base64
import kotlin.time.Instant

/**
 * A typed context-attribute value drawn from the CloudEvents type system.
 *
 * Every abstract type has a spec-defined canonical string form ([canonicalString]) and can be
 * reconstructed from it through the matching `fromCanonicalString` factory, so a value round-trips
 * between its native representation and the wire form. The same hierarchy backs both core and
 * extension attributes.
 */
sealed interface CloudEventAttributeValue {
    /** The spec-defined canonical string encoding of this value. */
    val canonicalString: String
}

/** The `Boolean` abstract type; canonical form is `"true"` or `"false"`. */
data class BooleanValue(val value: Boolean) : CloudEventAttributeValue {
    override val canonicalString: String get() = value.toString()

    companion object {
        /** @throws IllegalArgumentException if [text] is not exactly `"true"` or `"false"`. */
        fun fromCanonicalString(text: String): BooleanValue = BooleanValue(text.toBooleanStrict())
    }
}

/** The `Integer` abstract type: a signed 32-bit value; canonical form is its decimal string. */
data class IntegerValue(val value: Int) : CloudEventAttributeValue {
    override val canonicalString: String get() = value.toString()

    companion object {
        /** @throws NumberFormatException if [text] is not a signed 32-bit decimal integer. */
        fun fromCanonicalString(text: String): IntegerValue = IntegerValue(text.toInt())
    }
}

/** The `String` abstract type; the canonical form is the string itself. */
data class StringValue(val value: String) : CloudEventAttributeValue {
    override val canonicalString: String get() = value

    companion object {
        fun fromCanonicalString(text: String): StringValue = StringValue(text)
    }
}

/** The `URI` abstract type (an absolute URI); the canonical form is the string itself. */
data class UriValue(val value: String) : CloudEventAttributeValue {
    override val canonicalString: String get() = value

    companion object {
        fun fromCanonicalString(text: String): UriValue = UriValue(text)
    }
}

/** The `URI-reference` abstract type (relative permitted); the canonical form is the string itself. */
data class UriReferenceValue(val value: String) : CloudEventAttributeValue {
    override val canonicalString: String get() = value

    companion object {
        fun fromCanonicalString(text: String): UriReferenceValue = UriReferenceValue(text)
    }
}

/** The `Timestamp` abstract type; canonical form is an RFC 3339 date-time. */
data class TimestampValue(val value: Instant) : CloudEventAttributeValue {
    override val canonicalString: String get() = value.toString()

    companion object {
        /** @throws IllegalArgumentException if [text] is not a valid RFC 3339 date-time. */
        fun fromCanonicalString(text: String): TimestampValue = TimestampValue(Instant.parse(text))
    }
}

/**
 * The `Binary` abstract type; canonical form is Base64 (RFC 4648 §4).
 *
 * Equality is defined over the byte content, so two `BinaryValue`s with equal bytes are equal.
 */
class BinaryValue(value: ByteArray) : CloudEventAttributeValue {
    private val bytes: ByteArray = value.copyOf()

    /**
     * The bytes of this value, as a defensive copy: mutating the returned array does not change the
     * value, preserving its content-based equality and hash code.
     */
    val value: ByteArray get() = bytes.copyOf()

    override val canonicalString: String get() = Base64.encode(bytes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryValue) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = "BinaryValue($canonicalString)"

    companion object {
        /** @throws IllegalArgumentException if [text] is not valid Base64. */
        fun fromCanonicalString(text: String): BinaryValue = BinaryValue(Base64.decode(text))
    }
}
