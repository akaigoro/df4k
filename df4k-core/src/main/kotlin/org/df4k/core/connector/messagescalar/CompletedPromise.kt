package org.df4k.core.connector.messagescalar

import org.df4k.core.connector.messagescalar.ScalarPublisher
import org.df4k.core.connector.messagescalar.ScalarSubscriber

class CompletedPromise<M> : ScalarPublisher<M> {
    val value: M?
    val exception: Throwable?

    constructor(value: M) {
        this.value = value
        this.exception = null
    }

    constructor(value: M?, exception: Throwable) {
        if (value != null) {
            throw IllegalArgumentException("first argument must be null in the two-argument constructor")
        }
        this.value = null
        this.exception = exception
    }

    override fun <S : ScalarSubscriber<in M>> subscribe(subscriber: S): S {
        if (exception == null) {
            subscriber.post(value)
        } else {
            subscriber.postFailure(exception)
        }
        return subscriber
    }
}
