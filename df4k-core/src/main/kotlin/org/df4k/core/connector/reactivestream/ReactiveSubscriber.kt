package org.df4k.core.connector.reactivestream

import org.df4k.core.connector.messagestream.StreamCollector

/**
 * receiver of message stream with back pressure
 */
interface ReactiveSubscriber<T> : StreamCollector<T> {

    fun onSubscribe(subscription: ReactiveSubscription)
}
