// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

/**
 * Raised when a CloudEvent cannot be encoded to, or decoded from, the protobuf event format.
 *
 * Covers malformed documents (unparseable, truncated, or wire-malformed protobuf bytes, unknown
 * `spec_version`, a `data` payload declared `application/protobuf` that is not a valid `Any`),
 * structural ill-formedness, and semantic validation violations (wrapping the core exception).
 */
public class ProtobufEventFormatException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
