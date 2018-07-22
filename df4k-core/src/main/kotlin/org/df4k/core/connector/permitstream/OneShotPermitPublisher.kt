package org.df4k.core.connector.permitstream

import org.df4k.core.connector.messagescalar.SimpleSubscription

/**
 * allows only one subscriber
 */
class OneShotPermitPublisher : PermitPublisher, SimpleSubscription {
    internal var subscriber: PermitSubscriber? = null
    internal var permitCount: Int = 0

    override fun subscribe(subscriber: PermitSubscriber) {
        this.subscriber = subscriber
        subscriber.onSubscribe(this)
        movePermits(subscriber)
    }

    @Synchronized
    private fun movePermits(subscriber: PermitSubscriber) {
        subscriber.release(permitCount.toLong())
        permitCount = 0
    }

    @Synchronized
    fun release(n: Long) {
        if (subscriber == null) {
            permitCount += n.toInt()
        } else {
            subscriber!!.release(n)
        }
    }

    override fun cancel(): Boolean {
        subscriber = null
        return true
    }
}
