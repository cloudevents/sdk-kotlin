// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

/**
 * True when [contentType] declares JSON-formatted data per §3.1.1 — a media type whose subtype is
 * `json` or ends with the `+json` suffix, or null (which the spec says to proceed as if
 * `application/json`).
 */
internal fun isJsonContentType(contentType: String?): Boolean {
    if (contentType == null) return true
    val subtype = contentType.substringAfter('/', "").substringBefore(';').trim().lowercase()
    return subtype == "json" || subtype.endsWith("+json")
}
