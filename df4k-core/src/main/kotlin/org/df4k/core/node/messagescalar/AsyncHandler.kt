package org.df4k.core.node.messagescalar

import org.df4k.core.connector.messagescalar.ConstInput
import org.df4k.core.connector.messagescalar.ScalarSubscriber
import org.df4k.core.node.Action
import org.df4k.core.util.Pair

import java.util.function.*

class AsyncHandler<T, R>(private val action: BiFunction<in T, in Throwable, out R>) : AsyncSupplier<R>(), ScalarSubscriber<T> {
    private val argument = ConstInput<Pair<T, Throwable>>(this)

    override fun post(message: T) {
        argument.post(Pair(message, null))
    }

    override fun postFailure(throwable: Throwable) {
        argument.post(Pair(null, throwable))
    }

    @Action
    fun act(arg: Pair<T, Throwable>): R {
        return action.apply(arg.t, arg.u)
    }

}
