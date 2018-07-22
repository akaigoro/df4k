package org.df4k.core.connector.messagescalar

class SubscriberPromise<T> : CompletablePromise<T>(), ScalarSubscriber<T> {
    protected var subscription: SimpleSubscription? = null
    protected var cancelled = false

    override fun post(message: T) {
        complete(message)
    }

    override fun postFailure(ex: Throwable) {
        completeExceptionally(ex)
    }

    @Synchronized
    override fun onSubscribe(subscription: SimpleSubscription) {
        this.subscription = subscription
    }

    @Synchronized
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (subscription == null) {
            return cancelled
        }
        val subscription = this.subscription
        this.subscription = null
        cancelled = true
        return subscription!!.cancel()
    }

    @Synchronized
    override fun isCancelled(): Boolean {
        return cancelled
    }

}
