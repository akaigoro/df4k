package org.df4k.core.node.messagescalar

import org.df4k.core.connector.messagescalar.ConstInput
import org.df4k.core.util.invoker.BiConsumerInvoker
import org.df4k.core.util.invoker.BiFunctionInvoker

import java.util.function.BiConsumer
import java.util.function.BiFunction

open class AsyncBiFunction<U, V, R> : AsyncSupplier<R> {
    val param1 = ConstInput<U>(this)
    val param2 = ConstInput<V>(this)

    constructor(fn: BiFunction<in U, in V, out R>) : super(BiFunctionInvoker(fn)) {}

    constructor(action: BiConsumer<U, V>) : super(BiConsumerInvoker(action)) {}
}
