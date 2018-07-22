package org.df4k.core.util.invoker

import java.util.function.BiFunction

class BiFunctionInvoker<U, V, R>(function: BiFunction<U, V, R>) : AbstractInvoker<BiFunction<U, V, R>, R>(function) {

    override fun apply(vararg args: Any): R {
        assert(args.size == 2)
        return function!!.apply(args[0] as U, args[1] as V)
    }

    override fun returnsValue(): Boolean {
        return true
    }
}
