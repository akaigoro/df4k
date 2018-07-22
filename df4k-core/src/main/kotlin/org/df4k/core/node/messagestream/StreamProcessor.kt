package org.df4k.core.node.messagestream

import org.df4k.core.connector.messagestream.StreamOutput
import org.df4k.core.connector.messagestream.StreamPublisher
import org.df4k.core.connector.messagestream.StreamSubscriber
import org.df4k.core.node.Action

abstract class StreamProcessor<M, R> : Actor1<M>(), StreamPublisher<R> {
    protected val output = StreamOutput<R>(this)

    override fun <S : StreamSubscriber<in R>> subscribe(subscriber: S): S {
        output.subscribe(subscriber)
        return subscriber
    }

    @Action
    protected fun act(message: M?) {
        if (message == null) {
            output.complete()
        } else {
            val res = process(message)
            output.post(res)
        }
    }

    protected abstract fun process(message: M): R

}
