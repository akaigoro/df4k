package org.df4k.core.connector.messagescalar

import java.util.concurrent.Future

/**
 * this is a Promise interface
 * @param <T> the published item type
</T> */
@FunctionalInterface
interface ScalarPublisher<T> {
    /**
     * Adds the given Subscriber if possible.  If already
     * subscribed, or the attempt to subscribe fails due to policy
     * violations or errors, the Subscriber's `postFailure`
     * method is invoked with an [IllegalStateException].
     *
     * @param subscriber the subscriber
     * @throws NullPointerException if subscriber is null
     */
    fun <S : ScalarSubscriber<in T>> subscribe(subscriber: S): S

    fun asFuture(): Future<T> {
        return subscribe(SubscriberFuture())
    }

}
