package org.df4k.core.util.invoker

import java.lang.reflect.Method

class MethodInvoker<R>(private val actionObject: Any, private val actionMethod: Method?) : Invoker<R> {
    private val returnsValue: Boolean

    override val isEmpty: Boolean
        get() = actionMethod == null

    init {
        val rt = actionMethod.getReturnType()
        returnsValue = rt != Void.TYPE
    }

    @Throws(Exception::class)
    override fun apply(vararg args: Any): R {
        val res = actionMethod!!.invoke(actionObject, *args)
        return res as R
    }

    override fun returnsValue(): Boolean {
        return returnsValue
    }
}
