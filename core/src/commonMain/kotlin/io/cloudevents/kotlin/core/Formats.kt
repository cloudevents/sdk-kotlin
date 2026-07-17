// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

import kotlin.time.Instant

/**
 * Format and type checkers for the CloudEvents type system, shared by validation and, in future,
 * by event-format parsing. All checks run identically on every target from `commonMain`.
 */
@Suppress("TooManyFunctions") // Cohesive set of small single-purpose type/format checkers.
internal object Formats {
    private const val C0_CONTROL_END = 0x1F
    private const val DEL = 0x7F
    private const val C1_CONTROL_END = 0x9F
    private const val NONCHARACTER_BLOCK_START = 0xFDD0
    private const val NONCHARACTER_BLOCK_END = 0xFDEF
    private const val PLANE_NONCHARACTER_MASK = 0xFFFE
    private const val SUPPLEMENTARY_PLANE_BASE = 0x10000
    private const val HIGH_SURROGATE_START = 0xD800
    private const val LOW_SURROGATE_START = 0xDC00
    private const val SURROGATE_SHIFT = 10
    private const val PERCENT_ENCODING_LENGTH = 3

    private const val IPVFUTURE_MIN_DOT_INDEX = 2
    private const val IPV6_GROUP_COUNT = 8
    private const val IPV6_MAX_GROUPS_WITH_COMPRESSION = 7
    private const val H16_MAX_LENGTH = 4
    private const val IPV4_OCTET_COUNT = 4
    private const val IPV4_OCTET_MAX_LENGTH = 3
    private const val IPV4_OCTET_MAX_VALUE = 255
    private const val IPV4_GROUP_EQUIVALENT = 2

    private const val URI_GROUP_SCHEME = 1
    private const val URI_GROUP_AUTHORITY = 2
    private const val URI_GROUP_PATH = 3
    private const val URI_GROUP_QUERY = 4
    private const val URI_GROUP_FRAGMENT = 5

    private val URI_REFERENCE =
        Regex("^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?\$")

    /** True if [text] is a valid RFC 3339 date-time (an instant, so an offset is required). */
    fun isRfc3339(text: String): Boolean = runCatching { Instant.parse(text) }.isSuccess

    /** True if [text] parses as a signed 32-bit integer (rejects out-of-range and non-numeric). */
    fun isSignedInt32(text: String): Boolean = text.toIntOrNull() != null

    /** True if [text] is an RFC 3986 URI-reference (a relative reference is permitted). */
    fun isUriReference(text: String): Boolean = isWellFormedUri(text)

    /** True if [text] is an RFC 3986 absolute URI (a scheme is required). */
    fun isAbsoluteUri(text: String): Boolean = hasScheme(text) && isWellFormedUri(text)

    /**
     * Returns a human-readable reason if [text] is not a valid CloudEvents `String` value, or `null`
     * if it is valid. Rejects control characters (U+0000–U+001F, U+007F–U+009F), Unicode
     * noncharacters, and unpaired surrogates.
     */
    fun firstStringViolation(text: String): String? {
        val codePoints = decodeCodePoints(text) ?: return "contains an unpaired surrogate"
        return codePoints.firstNotNullOfOrNull(::codePointViolation)
    }

    private fun codePointViolation(codePoint: Int): String? = when {
        codePoint <= C0_CONTROL_END || codePoint in DEL..C1_CONTROL_END -> "contains a control character"
        isNoncharacter(codePoint) -> "contains a Unicode noncharacter"
        else -> null
    }

    private fun isNoncharacter(codePoint: Int): Boolean =
        codePoint in NONCHARACTER_BLOCK_START..NONCHARACTER_BLOCK_END ||
            (codePoint and PLANE_NONCHARACTER_MASK) == PLANE_NONCHARACTER_MASK

    /** Decodes UTF-16 to code points, returning `null` if an unpaired surrogate is encountered. */
    private fun decodeCodePoints(text: String): List<Int>? {
        val codePoints = ArrayList<Int>(text.length)
        var index = 0
        var valid = true
        while (index < text.length && valid) {
            val char = text[index]
            when {
                char.isHighSurrogate() -> {
                    val low = text.getOrNull(index + 1)
                    if (low != null && low.isLowSurrogate()) {
                        codePoints.add(codePointOf(char.code, low.code))
                        index += 2
                    } else {
                        valid = false
                    }
                }

                char.isLowSurrogate() -> valid = false

                else -> {
                    codePoints.add(char.code)
                    index++
                }
            }
        }
        return if (valid) codePoints else null
    }

    private fun codePointOf(high: Int, low: Int): Int =
        SUPPLEMENTARY_PLANE_BASE + ((high - HIGH_SURROGATE_START) shl SURROGATE_SHIFT) + (low - LOW_SURROGATE_START)

    private fun hasScheme(text: String): Boolean {
        val colon = text.indexOf(':')
        if (colon <= 0) return false
        val scheme = text.substring(0, colon)
        return scheme[0].isAsciiLetter() &&
            scheme.all { it.isAsciiLetter() || it.isAsciiDigit() || it == '+' || it == '-' || it == '.' }
    }

    /**
     * True if [text] satisfies the RFC 3986 URI-reference grammar. Beyond the allowed character set
     * and percent-encoding, the structure is validated: a syntactically valid scheme, an authority
     * whose host is a reg-name or a bracket-paired IP-literal, and a numeric port.
     */
    private fun isWellFormedUri(text: String): Boolean {
        val match = URI_REFERENCE.matchEntire(text) ?: return false
        return isValidUriParts(match)
    }

    private fun isValidUriParts(match: MatchResult): Boolean {
        val scheme = match.groupValues[URI_GROUP_SCHEME]
        val authority = match.groups[URI_GROUP_AUTHORITY]?.value
        val schemeOk = scheme.isEmpty() || isValidScheme(scheme)
        val authorityOk = authority == null || isValidAuthority(authority)
        val pathOk = validateComponent(match.groupValues[URI_GROUP_PATH]) { it.isPchar() || it == '/' }
        val queryOk = match.groups[URI_GROUP_QUERY]?.value?.let(::isQueryOrFragment) ?: true
        val fragmentOk = match.groups[URI_GROUP_FRAGMENT]?.value?.let(::isQueryOrFragment) ?: true
        return schemeOk && authorityOk && pathOk && queryOk && fragmentOk
    }

    private fun isValidScheme(scheme: String): Boolean = scheme[0].isAsciiLetter() &&
        scheme.all { it.isAsciiLetter() || it.isAsciiDigit() || it == '+' || it == '-' || it == '.' }

    /** Validates the `[ userinfo "@" ] host [ ":" port ]` grammar of an RFC 3986 authority. */
    private fun isValidAuthority(authority: String): Boolean {
        val at = authority.indexOf('@')
        val userinfoOk = at < 0 ||
            validateComponent(authority.substring(0, at)) {
                it.isUnreserved() || it.isSubDelim() || it == ':'
            }
        val hostPort = if (at < 0) authority else authority.substring(at + 1)
        return userinfoOk && isValidHostPort(hostPort)
    }

    private fun isValidHostPort(hostPort: String): Boolean {
        val split = splitHostPort(hostPort) ?: return false
        val (host, port) = split
        return (port == null || port.all { it.isAsciiDigit() }) && isValidHost(host)
    }

    /** Splits `host[:port]` into its parts, or returns `null` if an IP-literal bracket is unbalanced. */
    private fun splitHostPort(hostPort: String): Pair<String, String?>? = if (hostPort.startsWith("[")) {
        splitBracketedHostPort(hostPort)
    } else {
        val colon = hostPort.lastIndexOf(':')
        if (colon >= 0) hostPort.substring(0, colon) to hostPort.substring(colon + 1) else hostPort to null
    }

    private fun splitBracketedHostPort(hostPort: String): Pair<String, String?>? {
        val close = hostPort.indexOf(']')
        if (close < 0) return null
        val host = hostPort.substring(0, close + 1)
        val after = hostPort.substring(close + 1)
        return when {
            after.isEmpty() -> host to null
            after[0] == ':' -> host to after.substring(1)
            else -> null
        }
    }

    private fun isValidHost(host: String): Boolean = if (host.startsWith("[")) {
        host.endsWith("]") && isValidIpLiteral(host.substring(1, host.length - 1))
    } else {
        validateComponent(host) { it.isUnreserved() || it.isSubDelim() }
    }

    private fun isValidIpLiteral(content: String): Boolean =
        if (content.startsWith('v') || content.startsWith('V')) isValidIpvFuture(content) else isValidIpv6(content)

    private fun isValidIpvFuture(text: String): Boolean {
        val dot = text.indexOf('.')
        return dot >= IPVFUTURE_MIN_DOT_INDEX &&
            text.substring(1, dot).all { it.isAsciiHexDigit() } &&
            text.substring(dot + 1).let { tail ->
                tail.isNotEmpty() && tail.all { it.isUnreserved() || it.isSubDelim() || it == ':' }
            }
    }

    private fun isValidIpv6(text: String): Boolean {
        val singleMarker = text.indexOf("::") == text.lastIndexOf("::")
        val count = if (singleMarker) ipv6GroupCount(ipv6Groups(text)) else null
        return count != null &&
            if (text.contains("::")) count <= IPV6_MAX_GROUPS_WITH_COMPRESSION else count == IPV6_GROUP_COUNT
    }

    private fun ipv6Groups(text: String): List<String> {
        val marker = text.indexOf("::")
        val head = if (marker < 0) text else text.substring(0, marker)
        val tail = if (marker < 0) "" else text.substring(marker + 2)
        return (if (head.isEmpty()) emptyList() else head.split(":")) +
            (if (tail.isEmpty()) emptyList() else tail.split(":"))
    }

    /** Counts the 16-bit groups, treating a trailing IPv4 literal as two; `null` on any invalid group. */
    private fun ipv6GroupCount(groups: List<String>): Int? {
        var count = 0
        var valid = true
        groups.forEachIndexed { index, group ->
            val embeddedIpv4 = index == groups.lastIndex && group.contains(".")
            when {
                embeddedIpv4 && isValidIpv4(group) -> count += IPV4_GROUP_EQUIVALENT
                !embeddedIpv4 && isValidH16(group) -> count++
                else -> valid = false
            }
        }
        return if (valid) count else null
    }

    private fun isValidH16(group: String): Boolean =
        group.isNotEmpty() && group.length <= H16_MAX_LENGTH && group.all { it.isAsciiHexDigit() }

    private fun isValidIpv4(text: String): Boolean {
        val octets = text.split(".")
        return octets.size == IPV4_OCTET_COUNT && octets.all(::isValidIpv4Octet)
    }

    private fun isValidIpv4Octet(octet: String): Boolean = octet.isNotEmpty() &&
        octet.length <= IPV4_OCTET_MAX_LENGTH &&
        octet.all { it.isAsciiDigit() } &&
        octet.toInt() <= IPV4_OCTET_MAX_VALUE

    private fun isQueryOrFragment(text: String): Boolean =
        validateComponent(text) { it.isPchar() || it == '/' || it == '?' }

    /** Scans [text] left to right, allowing percent-encoded triplets and any char accepted by [allowed]. */
    private inline fun validateComponent(text: String, allowed: (Char) -> Boolean): Boolean {
        var index = 0
        var valid = true
        while (index < text.length && valid) {
            val char = text[index]
            when {
                char == '%' -> if (isPercentTripletAt(text, index)) index += PERCENT_ENCODING_LENGTH else valid = false
                allowed(char) -> index++
                else -> valid = false
            }
        }
        return valid
    }

    private fun isPercentTripletAt(text: String, index: Int): Boolean =
        index + PERCENT_ENCODING_LENGTH <= text.length &&
            text[index + 1].isAsciiHexDigit() &&
            text[index + 2].isAsciiHexDigit()

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

    private fun Char.isAsciiHexDigit(): Boolean = isAsciiDigit() || this in 'a'..'f' || this in 'A'..'F'

    private fun Char.isUnreserved(): Boolean =
        isAsciiLetter() || isAsciiDigit() || this == '-' || this == '.' || this == '_' || this == '~'

    private fun Char.isSubDelim(): Boolean = this == '!' ||
        this == '$' ||
        this == '&' ||
        this == '\'' ||
        this == '(' ||
        this == ')' ||
        this == '*' ||
        this == '+' ||
        this == ',' ||
        this == ';' ||
        this == '='

    private fun Char.isPchar(): Boolean = isUnreserved() || isSubDelim() || this == ':' || this == '@'
}
