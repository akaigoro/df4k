package org.df4k.core.util.invoker

interface Invoker<R> {
    val isEmpty: Boolean

    @Throws(Exception::class)
    fun apply(vararg args: Any): R

    open fun returnsValue(): Boolean {
        return false
    }
}
