package org.df4k.core.connector.messagestream

import org.df4k.core.connector.messagescalar.ScalarInput
import org.df4k.core.node.AsyncTask

import java.util.ArrayDeque
import java.util.Deque

/**
 * A Queue of tokens of type <T>
 *
 * @param <T>
</T></T> */
open class StreamInput<T> : ScalarInput<T>, StreamSubscriber<T>, Iterator<T> {
    protected var queue: Deque<T>
    protected var closeRequested = false

    open val isClosed: Boolean
        @Synchronized get() = closeRequested && value == null

    constructor(actor: AsyncTask) : super(actor) {
        this.queue = ArrayDeque()
    }

    constructor(actor: AsyncTask, capacity: Int) : super(actor) {
        this.queue = ArrayDeque(capacity)
    }

    constructor(actor: AsyncTask, queue: Deque<T>) : super(actor) {
        this.queue = queue
    }

    protected open fun size(): Int {
        return queue.size
    }

    @Synchronized
    override fun post(token: T?) {
        if (token == null) {
            throw NullPointerException()
        }
        if (closeRequested) {
            throw IllegalStateException("closed already")
        }
        if (exception != null) {
            throw IllegalStateException("token set already")
        }
        if (value == null) {
            value = token
            turnOn()
        } else {
            queue.add(token)
        }
    }

    /**
     * Signals the end of the stream. Turns this pin on. Removed value is
     * null (null cannot be send with Subscriber.add(message)).
     */
    @Synchronized
    override fun complete() {
        if (closeRequested) {
            return
        }
        closeRequested = true
        if (value == null) {
            turnOn()
        }
    }

    override fun pushback() {
        if (pushback) {
            throw IllegalStateException()
        }
        pushback = true
    }

    @Synchronized
    override fun pushback(value: T?) {
        if (value == null) {
            throw IllegalArgumentException()
        }
        if (!pushback) {
            pushback = true
        } else {
            if (this.value == null) {
                throw IllegalStateException()
            }
            queue.addFirst(this.value)
            this.value = value
        }
    }

    @Synchronized
    override fun next(): T? {
        if (pushback) {
            pushback = false
            return value // value remains the same, the pin remains turned on
        }
        val res = value
        val wasNull = value == null
        value = queue.poll()
        if (value == null) {
            // no more tokens; check closing
            if (wasNull || !closeRequested) {
                turnOff()
            }
        }
        return res
    }

    override fun hasNext(): Boolean {
        return value != null
    }
}
