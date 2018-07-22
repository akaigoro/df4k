package org.df4k.core.util.invoker

import java.util.function.Supplier

class SupplierInvoker<R>(supplier: Supplier<R>) : AbstractInvoker<Supplier<R>, R>(supplier) {

    override fun apply(vararg args: Any): R {
        assert(args.size == 0)
        return function!!.get()
    }

}
