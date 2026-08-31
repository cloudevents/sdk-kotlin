// SPDX-License-Identifier: Apache-2.0

@file:Suppress("MagicNumber")

package io.cloudevents.kotlin.protobuf

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Hand-written `@Serializable` mirror of the CloudEvents protobuf wire schema — the spec's
 * `cloudevents.proto` (vendored under `src/commonMain/proto/`). Field numbers MUST match the proto
 * or interop silently breaks; the vendored file is the source of truth.
 *
 * The kotlinx protobuf format ships no well-known types and no oneof, so:
 * - `google.protobuf.Timestamp` and `google.protobuf.Any` are modeled here ([ProtoTimestamp],
 *   [ProtoAny]) with the well-known field layouts;
 * - the `oneof data` / `oneof attr` groups are modeled as mutually-exclusive nullable fields
 *   (exactly one must be set; the codec enforces this instead of the wire);
 * - unknown top-level fields are dropped on decode, bounded in practice because the format's real
 *   extensibility seam — the `attributes` map — is preserved.
 */
@Suppress("LongParameterList") // Mirrors the proto message's fixed attribute set, one field per wire field.
@Serializable
internal data class ProtoCloudEvent(
    @ProtoNumber(1) val id: String = "",
    @ProtoNumber(2) val source: String = "",
    @ProtoNumber(3) val specVersion: String = "",
    @ProtoNumber(4) val type: String = "",
    @ProtoNumber(5) val attributes: Map<String, ProtoAttributeValue> = emptyMap(),
    @ProtoNumber(6) val binaryData: ByteArray? = null,
    @ProtoNumber(7) val textData: String? = null,
    @ProtoNumber(8) val protoData: ProtoAny? = null,
)

/** The `CloudEventAttributeValue` nested message: one of the seven type-system value branches. */
@Serializable
internal data class ProtoAttributeValue(
    @ProtoNumber(1) val ceBoolean: Boolean? = null,
    @ProtoNumber(2) val ceInteger: Int? = null,
    @ProtoNumber(3) val ceString: String? = null,
    @ProtoNumber(4) val ceBytes: ByteArray? = null,
    @ProtoNumber(5) val ceUri: String? = null,
    @ProtoNumber(6) val ceUriRef: String? = null,
    @ProtoNumber(7) val ceTimestamp: ProtoTimestamp? = null,
)

/** Hand-modeled `google.protobuf.Timestamp` (UTC instant as seconds + nanos). */
@Serializable
internal data class ProtoTimestamp(@ProtoNumber(1) val seconds: Long = 0, @ProtoNumber(2) val nanos: Int = 0)

/** Hand-modeled `google.protobuf.Any` (fully-qualified type URL + opaque value bytes). */
@Serializable
internal data class ProtoAny(
    @ProtoNumber(1) val typeUrl: String = "",
    @ProtoNumber(2) val value: ByteArray = ByteArray(0),
)

/** The `CloudEventBatch` envelope: `repeated CloudEvent events = 1`. */
@Serializable
internal data class ProtoBatch(@ProtoNumber(1) val events: List<ProtoCloudEvent> = emptyList())

/**
 * The wire field numbers of the vendored `cloudevents.proto` — the single source of truth for the
 * oneof selection logic ([WireScan], [AttributeValueCodec], [EventDecoder]). Referenced by name so
 * order/selection code reads the wire contract instead of bare literals.
 */
internal object WireFields {
    const val BINARY_DATA = 6
    const val TEXT_DATA = 7
    const val PROTO_DATA = 8
    const val ATTRIBUTES = 5
    const val CE_BOOLEAN = 1
    const val CE_INTEGER = 2
    const val CE_STRING = 3
    const val CE_BYTES = 4
    const val CE_URI = 5
    const val CE_URI_REF = 6
    const val CE_TIMESTAMP = 7
}

/** The shared protobuf format instance (default configuration). Missing proto3 scalar fields
 * (e.g. a Timestamp with only `seconds` set, as the canonical google encoder omits zero `nanos`)
 * decode to their constructor defaults, so google-produced messages parse as-is. */
internal val proto = ProtoBuf { }
