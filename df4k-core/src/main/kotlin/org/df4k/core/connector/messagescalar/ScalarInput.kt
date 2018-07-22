package org.df4k.core.connector.messagescalar

import org.df4k.core.node.AsyncTask

/**
 * Token storage with standard Subscriber<T> interface.
 * It has place for only one token.
 *
 * @param <T>
 * type of accepted tokens.
</T></T> */
open class ScalarInput<T>(protected var actor: AsyncTask) : ConstInput<T>(actor), Iterator<T> {
    protected var pushback = false // if true, do not consume

    // ===================== backend

    protected open fun pushback() {
        if (pushback) {
            throw IllegalStateException()
        }
        pushback = true
    }

    @Synchronized
    protected open fun pushback(value: T) {
        if (pushback) {
            throw IllegalStateException()
        }
        pushback = true
        this.value = value
    }

    override fun hasNext(): Boolean {
        return !isDone
    }

    override fun next(): T? {
        if (exception != null) {
            throw RuntimeException(exception)
        }
        if (value == null) {
            throw IllegalStateException()
        }
        val res = value
        if (pushback) {
            pushback = false
            // value remains the same, the pin remains turned on
        } else {
            value = null
            turnOff()
        }
        return res
    }
}
