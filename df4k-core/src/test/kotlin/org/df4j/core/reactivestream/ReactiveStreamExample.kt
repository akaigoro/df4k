/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.reactivestream

import org.df4j.core.connector.reactivestream.*
import org.df4j.core.node.Action
import org.df4j.core.node.messagestream.Actor
import org.junit.Test

import java.io.PrintStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ReactiveStreamExample {

    @Throws(InterruptedException::class)
    fun testSourceToSink(sourceNumber: Int, sinkNumber: Int) {
        var sinkNumber = sinkNumber
        val fin = CountDownLatch(3)
        val from = Source(sourceNumber, fin)
        val to1 = Sink(sinkNumber, fin)
        from.subscribe(to1)
        val to2 = Sink(sinkNumber, fin)
        from.subscribe(to2)
        from.start()
        assertTrue(fin.await(1, TimeUnit.SECONDS))
        // publisher always sends all tokens, even if all subscribers unsubscribed.
        sinkNumber = Math.min(sourceNumber, sinkNumber)
        assertEquals(sinkNumber.toLong(), to1.received.toLong())
        assertEquals(sinkNumber.toLong(), to2.received.toLong())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSourceFirst() {
        testSourceToSink(0, 1)
        testSourceToSink(2, 1)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSinkFirst() {
        testSourceToSink(1, 0)
        testSourceToSink(10, 11)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSameTime() {
        testSourceToSink(0, 0)
        testSourceToSink(0, 1)
        testSourceToSink(1, 0)
        testSourceToSink(1, 1)
        testSourceToSink(5, 5)
    }

    /**
     * emits totalNumber of Integers and closes the stream
     */
    internal class Source(totalNumber: Int, var fin: CountDownLatch) : Actor(), ReactivePublisher<Int> {
        var pub = ReactiveOutput<Int>(this)
        var `val` = 0

        init {
            this.`val` = totalNumber
        }

        override fun <S : ReactiveSubscriber<in Int>> subscribe(subscriber: S): S {
            pub.subscribe(subscriber)
            return subscriber
        }

        @Action
        fun act() {
            if (`val` == 0) {
                pub.complete()
                println("Source.pub.complete()")
                fin.countDown()
                stop()
            } else {
                //          ReactorTest.println("pub.post("+val+")");
                pub.post(`val`)
                `val`--
            }
        }
    }

    /**
     * receives totalNumber of Integers and cancels the subscription
     */
    internal class Sink(totalNumber: Int, val fin: CountDownLatch) : Actor(), ReactiveSubscriber<Int> {
        var totalNumber: Int = 0
        var subscriber: ReactiveInput<Int>
        var received = 0

        init {
            if (totalNumber == 0) {
                subscriber = ReactiveInput(this)
                subscriber.cancel()
                println("  sink: countDown")
                fin.countDown()
            } else {
                subscriber = ReactiveInput(this)
                this.totalNumber = totalNumber
                start()
            }
        }

        override fun onSubscribe(subscription: ReactiveSubscription) {
            subscriber.onSubscribe(subscription)
        }

        override fun post(message: Int?) {
            subscriber.post(message)
        }

        override fun postFailure(ex: Throwable) {
            subscriber.postFailure(ex)
        }

        override fun complete() {
            subscriber.complete()
        }

        @Action
        fun act(`val`: Int?) {
            //     ReactorTest.println("  Sink.current()="+val);
            if (`val` != null) {
                println("  sink: received $`val`")
                received++
                if (received < totalNumber) {
                    return
                }
                subscriber.cancel()
            }
            println("  sink: countDown")
            fin.countDown()
            stop()
        }
    }

    companion object {

        internal var out = System.out
        internal fun println(s: String) {
            out.println(s)
            out.flush()
        }
    }

}
