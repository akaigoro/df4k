package org.df4k.core.node.messagescalar

import org.df4k.core.connector.messagescalar.CompletablePromise
import org.df4k.core.connector.messagescalar.ScalarPublisher
import org.df4k.core.connector.messagescalar.ScalarSubscriber
import org.df4k.core.node.AsyncProcedure
import org.df4k.core.util.invoker.Invoker
import org.df4k.core.util.invoker.RunnableInvoker
import org.df4k.core.util.invoker.SupplierInvoker

import java.util.function.Supplier

/**
 * Base class for scalar nodes
 * Has predefined unbound output connector to keep the result of computation.
 *
 * Even if the computation does not produce a resulting value,
 * that connector is useful to monitor the end of the computation.
 *
 * @param <R>
</R> */
open class AsyncSupplier<R> : AsyncProcedure<R>, ScalarPublisher<R> {
    /** place for demands  */
    protected val result = CompletablePromise<R>()

    constructor() {}

    constructor(invoker: Invoker<R>) : super(invoker) {}

    constructor(proc: Supplier<R>) : super(SupplierInvoker<R>(proc)) {}

    constructor(proc: Runnable) : super(RunnableInvoker<R>(proc)) {}

    fun asyncResult(): CompletablePromise<R> {
        return result
    }

    override fun <S : ScalarSubscriber<in R>> subscribe(subscriber: S): S {
        result.subscribe(subscriber)
        return subscriber
    }

    protected fun complete(res: R): Boolean {
        return result.complete(res)
    }

    protected fun completeExceptionally(ex: Throwable): Boolean {
        return result.completeExceptionally(ex)
    }

    @Throws(Exception::class)
    override fun runAction(): R {
        val value = super.runAction()
        result.complete(value)
        return value
    }

    override fun toString(): String {
        return super.toString() + result.toString()
    }

}
