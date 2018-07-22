package org.df4k.core.util.invoker

import java.util.function.BiConsumer

class BiConsumerInvoker<U, V>(consumer: BiConsumer<U, V>) : AbstractInvoker<BiConsumer<U, V>, Void>(consumer) {

    override fun apply(vararg args: Any): Void? {
        assert(args.size == 2)
        function!!.accept(args[0] as U, args[1] as V)
        return null
    }

}
