package org.df4k.core.node.messagescalar

import org.df4k.core.connector.messagescalar.ConstInput
import org.df4k.core.connector.messagescalar.ScalarSubscriber
import org.df4k.core.util.invoker.*

import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

class AsyncFunction<T, R> : AsyncSupplier<R>, ScalarSubscriber<T> {
    protected val argument = ConstInput<T>(this)

    constructor() {}

    constructor(fn: Function<T, R>) : super(FunctionInvoker<T, R>(fn)) {}

    constructor(action: Consumer<in T>) : super(ConsumerInvoker<in T, R>(action)) {}

    constructor(supplier: Supplier<R>) : super(SupplierInvoker<R>(supplier)) {}

    constructor(action: Runnable) : super(RunnableInvoker<R>(action)) {}

    override fun post(message: T) {
        argument.post(message)
    }

    override fun postFailure(throwable: Throwable) {
        argument.postFailure(throwable)
    }
}
