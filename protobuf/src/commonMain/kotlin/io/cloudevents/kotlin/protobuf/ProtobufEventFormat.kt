// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.protobuf

import io.cloudevents.kotlin.core.CloudEvent

/**
 * Entry point for the CloudEvents protobuf event format.
 *
 * Exposes the structured-mode `application/cloudevents+protobuf` codec and its batch-mode
 * `application/cloudevents-batch+protobuf` envelope for every supported KMP target. The wire
 * schema is the spec's `cloudevents.proto` (vendored under `src/commonMain/proto/`), mirrored by
 * hand-written `@Serializable` classes over `kotlinx-serialization-protobuf` (same release train
 * as the JSON module's kotlinx.serialization); `google.protobuf.Timestamp` and `Any` are
 * hand-modeled because the kotlinx protobuf format ships no well-known types.
 *
 * The codec supports both wire versions on the same message schema (per the reference sdk-java
 * implementation): `spec_version` carries `"1.0"` or `"0.3"`, and the version-specific optional
 * attributes (`dataschema` vs `schemaurl`/`datacontentencoding`) ride the `attributes` map.
 * Data routing follows the reference serializer: an explicit [ProtobufEventData.Proto] payload or a
 * `datacontenttype` of `application/protobuf` (parsed as `Any`) routes to `proto_data`;
 * text-declared content (`text/` prefix, JSON, XML, `+json`/`+xml` suffix) routes to `text_data`; everything else
 * to `binary_data`.
 */
public object ProtobufEventFormat {
    /** The media type for a single structured-mode protobuf CloudEvent. */
    public val mediaType: String get() = "application/cloudevents+protobuf"

    /** The media type for a batch-mode `CloudEventBatch` envelope of CloudEvents. */
    public val batchMediaType: String get() = "application/cloudevents-batch+protobuf"

    /**
     * Encodes [event] into a structured-mode protobuf message.
     *
     * @throws ProtobufEventFormatException if [event] cannot be faithfully represented (a payload
     *   declared `application/protobuf` that is not a valid `Any`, or a text-declared
     *   payload that is not valid UTF-8).
     */
    public fun encodeToByteArray(event: CloudEvent): ByteArray = encodeEvent(event)

    /**
     * Decodes a structured-mode protobuf message into a [CloudEvent].
     *
     * A wire message that sets several members of a `oneof` group resolves to its last member,
     * matching conformant protobuf parsers. Decoding is strict by default — an invalid event throws.
     *
     * @throws ProtobufEventFormatException if [bytes] is not a valid CloudEvents protobuf document
     *   (unparseable, truncated, or malformed bytes, unknown `spec_version`, a wire-structure
     *   violation, or a semantic violation of the event's own version).
     */
    public fun decodeFromByteArray(bytes: ByteArray): CloudEvent = decodeEvent(bytes)

    /**
     * Encodes a collection of events into a `CloudEventBatch` message. Each element carries its own
     * `spec_version` (mixed versions are allowed); an empty collection encodes to a batch with no
     * `events` field.
     */
    public fun encodeBatch(events: List<CloudEvent>): ByteArray = encodeBatchEvents(events)

    /**
     * Decodes a `CloudEventBatch` message into a list of events.
     *
     * @throws ProtobufEventFormatException if [bytes] is not a valid CloudEvents protobuf batch.
     */
    public fun decodeBatch(bytes: ByteArray): List<CloudEvent> = decodeBatchEvents(bytes)
}
