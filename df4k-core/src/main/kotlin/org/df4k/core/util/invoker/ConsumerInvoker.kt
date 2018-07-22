package org.df4k.core.util.invoker

import java.util.function.Consumer

class ConsumerInvoker<U, R>(consumer: Consumer<U>) : AbstractInvoker<Consumer<U>, R>(consumer) {

    override fun apply(vararg args: Any): R? {
        assert(args.size == 1)
        function!!.accept(args[0] as U)
        return null
    }

}
