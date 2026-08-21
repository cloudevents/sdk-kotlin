// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import io.cloudevents.kotlin.core.CloudEvent

/**
 * Entry point for the CloudEvents JSON event format.
 *
 * Fixes the public surface the SDK exposes for the structured-mode JSON format (and its batch
 * envelope). The signatures are defined here so the wire codec in later phases fills in the bodies
 * without reworking the module API; no encoding/decoding logic exists in this phase.
 */
public object JsonEventFormat {
    /** The IANA registered media type for a single structured-mode JSON CloudEvent. */
    public val mediaType: String get() = "application/cloudevents+json"

    /**
     * Encodes a [CloudEvent] into a structured-mode JSON document.
     *
     * @param event the event to encode
     * @return the JSON encoding of [event]
     */
    public fun encodeToString(event: CloudEvent): String = TODO("JSON encode of $event lands in Phase 6")

    /**
     * Decodes a structured-mode JSON document into a [CloudEvent].
     *
     * @param json the JSON document to decode
     * @return the decoded event
     */
    public fun decodeFromString(json: String): CloudEvent = TODO("JSON decode of $json lands in Phase 7")

    /**
     * Encodes a collection of events into a batch-mode JSON array.
     *
     * @param events the events to encode
     * @return the JSON array encoding of [events]
     */
    public fun encodeBatch(events: List<CloudEvent>): String = TODO("JSON batch encode of $events lands in Phase 8")
}
