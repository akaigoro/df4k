package org.df4k.core.connector.messagestream

import org.df4k.core.connector.messagescalar.SimpleSubscription

interface StreamSubscriber<T> : StreamCollector<T> {

    fun onSubscribe(subscription: SimpleSubscription)

}
