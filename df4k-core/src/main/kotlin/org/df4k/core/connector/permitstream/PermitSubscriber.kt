package org.df4k.core.connector.permitstream

import org.df4k.core.connector.messagescalar.SimpleSubscription
import org.df4k.core.connector.reactivestream.ReactiveSubscription

/**
 * inlet for permits.
 *
 * method descriptions are taken from description of class [ReactiveSubscription].
 */
interface PermitSubscriber {
    /**
     * Adds the given number `n` of items to the current
     * unfulfilled demand for this subscription.  If `n` is
     * less than or equal to zero, the Subscriber will receive an
     * `postFailure` signal with an [ ] argument.  Otherwise, the
     * Subscriber will receive up to `n` additional `post` invocations (or fewer if terminated).
     *
     * @param n the increment of demand; a value of `Long.MAX_VALUE` may be considered as effectively unbounded
     */
    fun release(n: Long)

    fun release() {
        release(1)
    }

    fun onSubscribe(subscription: SimpleSubscription)

}
