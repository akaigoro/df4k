package org.df4k.core.connector.messagestream

interface StreamPublisher<M> {

    fun <S : StreamSubscriber<in M>> subscribe(subscriber: S): S
}
