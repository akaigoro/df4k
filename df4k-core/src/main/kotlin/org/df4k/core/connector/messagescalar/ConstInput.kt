package org.df4k.core.connector.messagescalar

import org.df4k.core.node.AsyncTask

/**
 * Token storage with standard Subscriber<T> interface. It has place for only one
 * token, which is never consumed.
 *
 * @param <T>
 * type of accepted tokens.
</T></T> */
open class ConstInput<T>(actor: AsyncTask) : AsyncTask.AsynctParam<T>(), ScalarSubscriber<T> {
    protected var subscription: SimpleSubscription? = null
    protected var closeRequested = false
    protected var cancelled = false

    /** extracted token  */
    protected var completed = false
    var value: T? = null
        protected set
    var exception: Throwable? = null
        protected set

    val isDone: Boolean
        get() = completed || exception != null

    init {
        actor.`super`()
    }

    @Synchronized
    override fun onSubscribe(subscription: SimpleSubscription) {
        if (closeRequested) {
            subscription.cancel()
        } else {
            this.subscription = subscription
        }
    }

    @Synchronized
    fun current(): T? {
        if (exception != null) {
            throw IllegalStateException(exception)
        }
        return value
    }

    /**
     * pin bit remains ready
     */
    override fun next(): T? {
        return current()
    }

    override fun post(message: T?) {
        if (message == null) {
            throw IllegalArgumentException()
        }
        if (isDone) {
            throw IllegalStateException("token set already")
        }
        value = message
        turnOn()
    }

    override fun postFailure(throwable: Throwable) {
        if (isDone) {
            throw IllegalStateException("token set already")
        }
        this.exception = throwable
    }

    @Synchronized
    fun cancel(): Boolean {
        if (subscription == null) {
            return cancelled
        }
        val subscription = this.subscription
        this.subscription = null
        cancelled = true
        return subscription!!.cancel()
    }
}
