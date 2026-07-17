// SPDX-License-Identifier: Apache-2.0

package io.cloudevents.kotlin.core

/**
 * Builds a format- or protocol-specific representation [R] by visiting a CloudEvent's context
 * attributes and payload.
 *
 * This is the sink side of the message SPI: event formats and protocol bindings implement it to
 * serialize an event (the `Event → Message → Transport` flow). Attribute values are supplied
 * through the shared [CloudEventAttributeValue] type system, so a writer never depends on the
 * concrete [CloudEvent] implementation. Core ships no implementation.
 */
interface CloudEventWriter<out R> {
    /**
     * Sets a context attribute (core or extension) by its canonical name and typed value.
     *
     * @return this writer, so attribute writes can be chained before [end].
     */
    fun withContextAttribute(name: String, value: CloudEventAttributeValue): CloudEventWriter<R>

    /** Completes the event with its optional [data] payload and produces the representation [R]. */
    fun end(data: CloudEventData?): R
}

/**
 * Creates a [CloudEventWriter] once the wire [SpecVersion] of the event being read is known, so a
 * reader can pick a version-appropriate writer before any attribute is visited.
 */
fun interface CloudEventWriterFactory<out R> {
    fun create(specVersion: SpecVersion): CloudEventWriter<R>
}

/**
 * Reads a protocol message or serialized event and drives a [CloudEventWriter] to reproduce it,
 * yielding whatever representation [R] the writer builds.
 *
 * This is the source side of the message SPI: event formats and protocol bindings implement it to
 * deserialize (the `Transport → Message → Event` flow). Passing a writer factory that builds a
 * [CloudEvent] turns any reader into an event decoder. Core ships no implementation.
 */
interface MessageReader {
    fun <R> read(writerFactory: CloudEventWriterFactory<R>): R
}
