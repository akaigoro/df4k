package org.df4k.core.connector.permitstream

import org.df4k.core.connector.messagescalar.SimpleSubscription
import org.df4k.core.node.AsyncTask

/**
 * Counting semaphore
 * holds token counter without data.
 * counter can be negative.
 */
open class Semafor @JvmOverloads constructor(protected val actor: AsyncTask, count: Int = 0) : AsyncTask.Lock(), PermitSubscriber {
    var count: Long = 0
        private set
    protected var subscription: SimpleSubscription

    init {
        actor.`super`(count <= 0)
        this.count = count.toLong()
    }

    /** increments resource counter by delta  */
    @Synchronized
    override fun release(delta: Long) {
        if (delta < 0) {
            throw IllegalArgumentException("resource counter delta must be >= 0")
        }
        val prev = count
        count += delta
        if (prev <= 0 && count > 0) {
            turnOn()
        }
    }

    override fun onSubscribe(subscription: SimpleSubscription) {
        this.subscription = subscription
    }

    /** decrements resource counter by delta  */
    @Synchronized
    protected fun acquire(delta: Long) {
        if (delta <= 0) {
            throw IllegalArgumentException("resource counter delta must be > 0")
        }
        val prev = count
        count -= delta
        if (prev > 0 && count <= 0) {
            turnOff()
        }
    }

    @Synchronized
    fun drainPermits() {
        count = 0
        turnOff()
    }

    @Synchronized
    override fun purge() {
        acquire(1)
    }
}
