package org.df4k.core.node.messagescalar

import org.df4k.core.connector.messagescalar.CompletablePromise
import org.df4k.core.connector.messagescalar.ScalarSubscriber

class AllOf : AsyncSupplier<Array<Any>> {
    internal var results: Array<Any>

    constructor() {}

    constructor(vararg sources: CompletablePromise<*>) {
        results = arrayOfNulls(sources.size)
        for (k in sources.indices) {
            val source = sources[k]
            val arg = Enter(k)
            source.subscribe(arg)
        }
    }

    override fun fire() {
        complete(results)
    }

    internal inner class Enter(private val num: Int) : AsyncTask.Lock(), ScalarSubscriber<Any> {

        override fun post(value: Any) {
            results[num] = value
            super.turnOn()
        }

        override fun postFailure(ex: Throwable) {
            synchronized(this@AllOf) {
                if (!result.isDone) {
                    this@AllOf.completeExceptionally(ex)
                }
            }
        }
    }

}
