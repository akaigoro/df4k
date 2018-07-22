package org.df4k.core.connector.reactivestream

import org.df4k.core.connector.messagestream.StreamCollector
import org.df4k.core.connector.permitstream.Semafor
import org.df4k.core.node.AsyncTask

import java.util.HashSet
import java.util.function.Consumer

/**
 * serves multiple subscribers
 * demonstrates usage of class AsyncProc.Semafor for handling back pressure
 *
 * An equivalent to java.util.concurrent.SubmissionPublisher
 *
 * @param <M>
</M> */
class ReactiveOutput<M>(protected var base: AsyncTask) : AsyncTask.Lock(), ReactivePublisher<M>, StreamCollector<M> {
    protected var subscriptions: MutableSet<SimpleReactiveSubscriptionImpl>? = HashSet()

    init {
        base.`super`(false)
    }

    override fun <S : ReactiveSubscriber<in M>> subscribe(subscriber: S): S {
        val newSubscription = SimpleReactiveSubscriptionImpl(subscriber)
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

    fun forEachSubscription(operator: Consumer<in SimpleReactiveSubscriptionImpl>) {
        if (closed()) {
            return  // completed already
        }
        subscriptions!!.forEach(operator)
    }

    override fun post(item: M) {
        forEachSubscription({ subscription -> subscription.post(item) })
    }

    override fun postFailure(throwable: Throwable) {
        forEachSubscription({ subscription -> subscription.postFailure(throwable) })
    }

    @Synchronized
    override fun complete() {
        forEachSubscription(Consumer { it.complete() })
    }

    internal inner class SimpleReactiveSubscriptionImpl(protected var subscriber: ReactiveSubscriber<in M>?) : Semafor(base), ReactiveSubscription {
        @Volatile
        private var closed = false

        private val isCompleted: Boolean
            get() = subscriber == null

        init {
            if (subscriber == null) {
                throw NullPointerException()
            }
        }

        fun post(message: M) {
            if (isCompleted) {
                throw IllegalStateException("post to completed connector")
            }
            subscriber!!.post(message)
        }

        fun postFailure(throwable: Throwable) {
            if (isCompleted) {
                throw IllegalStateException("postFailure to completed connector")
            }
            subscriber!!.postFailure(throwable)
            cancel()
        }

        /**
         * does nothing: counter decreases when a message is posted
         */
        override fun purge() {}

        /**
         * subscription closed by request of publisher
         * unregistering not needed
         */
        fun complete() {
            if (isCompleted) {
                return
            }
            subscriber!!.complete()
            subscriber = null
        }

        /**
         * subscription closed by request of subscriber
         */
        @Synchronized
        override fun cancel(): Boolean {
            if (closed) {
                return false
            }
            closed = true
            subscriptions!!.remove(this)
            super.unRegister() // and cannot be turned on
            return false
        }

        override fun request(n: Long) {
            super.release(n)
        }
    }

}
