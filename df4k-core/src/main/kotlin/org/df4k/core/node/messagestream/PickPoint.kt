package org.df4k.core.node.messagestream

import org.df4k.core.connector.messagescalar.CompletablePromise
import org.df4k.core.connector.messagescalar.ScalarPublisher
import org.df4k.core.connector.messagescalar.ScalarSubscriber
import org.df4k.core.connector.messagescalar.SimpleSubscription
import org.df4k.core.connector.messagestream.StreamCollector
import org.df4k.core.connector.messagestream.StreamSubscriber

import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.*
import java.util.function.Function

/**
 * An asynchronous analogue of BlockingQueue
 * (only on output end, while from the input side it does not block)
 * @param <T>
</T> */
open class PickPoint<T> : StreamSubscriber<T>, ScalarPublisher<T>, BlockingQueue<T> {
    @get:Synchronized
    var isCompleted = false
        private set
    /** place for demands  */
    private var requests: Queue<ScalarSubscriber<in T>>? = ArrayDeque()
    /** place for resources  */
    private var resources: Queue<T>? = ArrayDeque()

    private var subscription: SimpleSubscription? = null

    override fun onSubscribe(subscription: SimpleSubscription) {
        this.subscription = subscription
    }

    @Synchronized
    override fun post(token: T) {
        if (isCompleted) {
            throw IllegalStateException()
        }
        if (requests!!.isEmpty()) {
            resources!!.add(token)
        } else {
            requests!!.poll().post(token)
        }
    }

    @Synchronized
    override fun complete() {
        if (isCompleted) {
            return
        }
        isCompleted = true
        resources = null
        for (subscriber in requests!!) {
            subscriber.postFailure(StreamCompletedException())
        }
        requests = null
    }

    override fun <S : ScalarSubscriber<in T>> subscribe(subscriber: S): S {
        if (isCompleted) {
            throw IllegalStateException()
        }
        if (resources!!.isEmpty()) {
            requests!!.add(subscriber)
        } else {
            subscriber.post(resources!!.poll())
        }
        return subscriber
    }

    /**====================== implementation of synchronous BlockingQueu interface  ==================== */

    override fun add(t: T): Boolean {
        post(t)
        return true
    }

    override fun offer(t: T): Boolean {
        post(t)
        return true
    }

    @Synchronized
    override fun remove(): T {
        return resources!!.remove()
    }

    @Synchronized
    override fun poll(): T? {
        return resources!!.poll()
    }

    @Synchronized
    override fun element(): T {
        return resources!!.element()
    }

    @Synchronized
    override fun peek(): T? {
        return resources!!.peek()
    }

    @Throws(InterruptedException::class)
    override fun put(t: T) {
        post(t)
    }

    @Synchronized
    @Throws(InterruptedException::class)
    override fun offer(t: T, timeout: Long, unit: TimeUnit): Boolean {
        post(t)
        return true
    }

    @Throws(InterruptedException::class)
    override fun take(): T {
        synchronized(this) {
            if (!resources!!.isEmpty() && requests!!.isEmpty()) {
                return resources!!.remove()
            }
        }
        val future = CompletablePromise<T>()
        subscribe(future)
        try {
            return future.get()
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        }

    }

    @Throws(InterruptedException::class)
    override fun poll(timeout: Long, unit: TimeUnit): T? {
        synchronized(this) {
            if (!resources!!.isEmpty() && requests!!.isEmpty()) {
                return resources!!.remove()
            }
        }
        val future = CompletablePromise<T>()
        subscribe(future)
        try {
            return future.get(timeout, unit)
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        } catch (e: TimeoutException) {
            throw RuntimeException(e)
        }

    }

    @Synchronized
    override fun remainingCapacity(): Int {
        return 1
    }

    @Synchronized
    override fun remove(o: Any): Boolean {
        return resources!!.remove(o)
    }

    override fun containsAll(c: Collection<*>): Boolean {
        return resources!!.containsAll(c)
    }

    @Synchronized
    override fun addAll(c: Collection<T>): Boolean {
        return resources!!.addAll(c)
    }

    @Synchronized
    override fun removeAll(c: Collection<*>): Boolean {
        return resources!!.removeAll(c)
    }

    override fun retainAll(c: Collection<*>): Boolean {
        return resources!!.retainAll(c)
    }

    @Synchronized
    override fun clear() {
        resources!!.clear()
    }

    @Synchronized
    override fun size(): Int {
        return resources!!.size
    }

    @Synchronized
    override fun isEmpty(): Boolean {
        return resources!!.isEmpty()
    }

    @Synchronized
    override operator fun contains(o: Any): Boolean {
        return resources!!.contains(o)
    }

    @Synchronized
    override fun iterator(): Iterator<T> {
        return resources!!.iterator()
    }

    @Synchronized
    override fun toArray(): Array<Any> {
        return resources!!.toTypedArray()
    }

    @Synchronized
    override fun <T1> toArray(a: Array<T1>): Array<T1> {
        return resources!!.toTypedArray<T1>()
    }

    @Synchronized
    override fun drainTo(c: Collection<in T>): Int {
        throw UnsupportedOperationException()
    }

    @Synchronized
    override fun drainTo(c: Collection<in T>, maxElements: Int): Int {
        throw UnsupportedOperationException()
    }

    internal class ThreadSupscriber
}
