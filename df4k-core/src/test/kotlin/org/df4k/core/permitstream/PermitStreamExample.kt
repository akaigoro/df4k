package org.df4k.core.permitstream

import org.df4k.core.connector.messagestream.StreamOutput
import org.df4k.core.connector.permitstream.OneShotPermitPublisher
import org.df4k.core.connector.permitstream.Semafor
import org.df4k.core.node.Action
import org.df4k.core.node.messagestream.Actor
import org.df4k.core.node.messagestream.Actor1
import org.df4k.core.node.messagestream.StreamProcessor
import org.df4k.core.util.SameThreadExecutor
import org.junit.Test

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import org.junit.Assert.assertEquals

/**
 * This is a demonstration how backpressure can be implemented using [Semafor]
 */
class PermitStreamExample {

    @Test
    @Throws(InterruptedException::class)
    fun piplineTest() {
        val totalCount = 10
        val first = Source(totalCount)
        val last = Sink()
        first.pub
                .subscribe(TestProcessor())
                .subscribe(TestProcessor())
                .subscribe(last).backPressureCommander
                .subscribe(first.backPressureActuator)
        first.start()
        last.fin.await(2, TimeUnit.SECONDS)
        assertEquals(totalCount.toLong(), last.totalCount.toLong())
    }

    class Source internal constructor(internal var count: Int) : Actor() {
        internal var backPressureActuator = Semafor(this)
        internal var pub = StreamOutput<Int>(this)

        init {
            setExecutor(SameThreadExecutor())
        }

        @Action
        fun act() {
            if (count == 0) {
                pub.complete()
            } else {
                pub.post(count)
                count--
            }
        }
    }

    internal class TestProcessor : StreamProcessor<Int, Int>() {
        init {
            start()
        }

        protected override fun process(message: Int?): Int? {
            return message
        }
    }

    internal class Sink : Actor1<Int>() {
        var backPressureCommander = OneShotPermitPublisher()
        var totalCount = 0
        var fin = CountDownLatch(1)

        init {
            backPressureCommander.release(1)
            start()
        }

        @Action
        @Throws(Exception::class)
        protected fun act(message: Int?) {
            if (message == null) {
                fin.countDown()
            } else {
                totalCount++
                backPressureCommander.release(1)
            }
        }
    }
}
