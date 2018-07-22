/**
 * This package contain interfaces to support unbounded stream of messages.
 * Each stream contains any number of calls to [org.df4k.core.connector.messagestream.StreamCollector.post]
 * and finishes with either call to [org.df4k.core.connector.messagestream.StreamCollector.postFailure] }
 * or [org.df4k.core.connector.messagestream.StreamCollector.complete] }
 */
package org.df4k.core.connector.messagestream

