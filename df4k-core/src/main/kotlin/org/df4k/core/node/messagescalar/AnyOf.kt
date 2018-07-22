package org.df4k.core.node.messagescalar

import org.df4k.core.connector.messagescalar.CompletablePromise
import org.df4k.core.connector.messagescalar.ScalarSubscriber

class AnyOf<T> : CompletablePromise<T> {

    constructor() {}

    constructor(vararg sources: CompletablePromise<out T>) {
        for (source in sources) {
            source.subscribe(Enter())
        }
    }

    internal inner class Enter : ScalarSubscriber<T> {
        override fun post(value: T) {
            synchronized(this@AnyOf) {
                if (!isDone) {
                    complete(value)
                }
            }
        }

        override fun postFailure(ex: Throwable) {
            synchronized(this@AnyOf) {
                if (!isDone) {
                    completeExceptionally(ex)
                }
            }
        }
    }

}
