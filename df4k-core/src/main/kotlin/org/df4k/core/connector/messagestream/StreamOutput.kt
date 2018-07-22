package org.df4k.core.connector.messagestream

import org.df4k.core.connector.messagescalar.SimpleSubscription
import org.df4k.core.node.AsyncTask

import java.util.HashSet
import java.util.function.Consumer

/**
 * serves multiple subscribers
 *
 * @param <M>
</M> */
class StreamOutput<M>(protected var base: AsyncTask) : AsyncTask.Lock(), StreamPublisher<M>, StreamCollector<M> {
    protected var subscriptions: MutableSet<SimpleSubscriptionImpl>? = HashSet()

    init {
        base.`super`(false)
    }

    override fun <S : StreamSubscriber<in M>> subscribe(subscriber: S): S {
        val newSubscription = SimpleSubscriptionImpl(subscriber)
        subscriptions!!.add(newSubscription)
        subscriber.onSubscribe(newSubscription)
        return subscriber
    }

    @Synchronized
    fun close() {
        subscriptions = null
        super.turnOff()
    }

    @Synchronized
    fun closed(): Boolean {
        return super.isBlocked
    }

    fun forEachSubscription(operator: Consumer<in SimpleSubscriptionImpl>) {
        if (closed()) {
            return  // completed already
        }
        subscriptions!!.forEach(operator)
    }

    override fun post(item: M?) {
        if (item == null) {
            throw NullPointerException()
        }
        forEachSubscription({ subscription -> subscription.post(item) })
    }

    override fun postFailure(throwable: Throwable) {
        forEachSubscription({ subscription -> subscription.postFailure(throwable) })
    }

    @Synchronized
    override fun complete() {
        forEachSubscription(Consumer { it.complete() })
    }

    internal inner class SimpleSubscriptionImpl(protected var subscriber: StreamSubscriber<in M>?) : SimpleSubscription {
        @Volatile
        private var closed = false

        fun post(message: M?) {
            subscriber!!.post(message)
        }

        fun postFailure(throwable: Throwable) {
            subscriber!!.postFailure(throwable)
            cancel()
        }

        /**
         * subscription closed by request of publisher
         * unregistering not needed
         */
        fun complete() {
            if (subscriber == null) {
                return
            }
            subscriber!!.complete()
            subscriber = null
        }

        /**
         * subscription closed by request of subscriber
         */
        override fun cancel(): Boolean {
            synchronized(this@StreamOutput) {
                if (closed) {
                    return false
                }
                closed = true
                subscriptions!!.remove(this)
                return false
            }
        }
    }

}
