package org.df4k.core.util.invoker

import java.util.function.Function

class FunctionInvoker<U, R>(function: Function<U, R>) : AbstractInvoker<Function<U, R>, R>(function) {

    override fun apply(vararg args: Any): R {
        assert(args.size == 1)
        return function!!.apply(args[0] as U)
    }

    override fun returnsValue(): Boolean {
        return true
    }

}
