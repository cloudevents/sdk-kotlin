// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.json

import io.cloudevents.kotlin.core.CloudEvent

/**
 * Entry point for the CloudEvents JSON event format.
 *
 * Exposes the structured-mode `application/cloudevents+json` codec and its batch-mode
 * `application/cloudevents-batch+json` envelope for every supported KMP target. The codec routes data
 * per the module's data-origin carrier (ADR 0005), reuses core's type/format checkers (ADR 0006), and
 * canonicalizes timestamps to UTC (ADR 0007).
 */
public object JsonEventFormat {
    /** The IANA registered media type for a single structured-mode JSON CloudEvent. */
    public val mediaType: String get() = "application/cloudevents+json"

    /** The IANA registered media type for a batch-mode JSON array of CloudEvents. */
    public val batchMediaType: String get() = BATCH_MEDIA_TYPE

    /**
     * Encodes [event] into a structured-mode JSON document.
     *
     * @throws JsonEventFormatException if [event] cannot be faithfully represented (a payload declared
     *   JSON that is not a valid JSON document).
     */
    public fun encodeToString(event: CloudEvent): String = encodeEvent(event)

    /**
     * Decodes a structured-mode JSON document into a [CloudEvent].
     *
     * @throws JsonEventFormatException if [json] is not a valid CloudEvents JSON document (wrong shape,
     *   unknown `specversion`, or a semantic violation of the event's own version).
     */
    public fun decodeFromString(json: String): CloudEvent = decodeEvent(json)

    /**
     * Encodes a collection of events into a batch-mode JSON array. Each element carries its own
     * `specversion` (mixed versions are allowed); an empty collection encodes to `[]`.
     */
    public fun encodeBatch(events: List<CloudEvent>): String = encodeBatchEvents(events)

    /**
     * Decodes a batch-mode JSON array into a list of events.
     *
     * @throws JsonEventFormatException if [json] is not a valid CloudEvents batch document.
     */
    public fun decodeBatch(json: String): List<CloudEvent> = decodeBatchEvents(json)
}
