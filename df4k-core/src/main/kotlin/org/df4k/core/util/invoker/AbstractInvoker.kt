package org.df4k.core.util.invoker

abstract class AbstractInvoker<FT, R> protected constructor(protected val function: FT?) : Invoker<R> {

    override val isEmpty: Boolean
        get() = function == null

    @Throws(Exception::class)
    abstract override fun apply(vararg args: Any): R
}
