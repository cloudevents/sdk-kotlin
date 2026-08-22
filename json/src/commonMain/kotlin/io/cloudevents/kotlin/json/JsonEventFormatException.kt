// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

/**
 * Raised when a CloudEvent cannot be encoded to, or decoded from, the JSON event format.
 *
 * Covers malformed documents (wrong shape, unknown `specversion`, invalid Base64 or RFC 3339 values),
 * the §3.1.1 `data`/`data_base64` contradiction, structural ill-formedness, and — on encode of a
 * payload declared JSON that is not a valid JSON document — [cause] carries the underlying parse error.
 */
public class JsonEventFormatException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
