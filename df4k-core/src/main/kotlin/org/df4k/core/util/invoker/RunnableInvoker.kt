package org.df4k.core.util.invoker

class RunnableInvoker<R>(runnable: Runnable) : AbstractInvoker<Runnable, R>(runnable) {

    override fun apply(vararg args: Any): R? {
        assert(args.size == 0)
        function!!.run()
        return null
    }

}
