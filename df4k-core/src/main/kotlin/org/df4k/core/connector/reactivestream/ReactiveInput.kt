package org.df4k.core.connector.reactivestream

import org.df4k.core.connector.messagestream.StreamInput
import org.df4k.core.node.AsyncTask

import java.util.ArrayDeque
import java.util.Deque

/**
 * A Queue of tokens of type <T>
 *
 * @param <T>
</T></T> */
class ReactiveInput<T> @JvmOverloads constructor(actor: AsyncTask, protected var capacity: Int = 8) : StreamInput<T>(actor), ReactiveSubscriber<T>, Iterator<T> {
    protected var queue: Deque<T>
    protected var closeRequested = false
    protected var subscription: ReactiveSubscription? = null

    override val isClosed: Boolean
        @Synchronized get() = closeRequested && value == null

    init {
        this.queue = ArrayDeque<T>(capacity)
    }

    override fun size(): Int {
        return queue.size
    }

    override fun onSubscribe(subscription: ReactiveSubscription) {
        this.subscription = subscription
        subscription.request(capacity.toLong())
    }

    @Synchronized
    override fun post(token: T?) {
        if (subscription == null) {
            throw IllegalStateException("not yet subscribed")
        }
        if (queue.size >= capacity) {
            throw IllegalStateException("no space for next token")
        }
        super.post(token)
    }

    @Synchronized
    override fun next(): T? {
        subscription!!.request(1)
        return super.next()
    }
}
