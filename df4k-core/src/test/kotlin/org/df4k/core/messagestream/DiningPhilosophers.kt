package org.df4k.core.messagestream

import org.df4k.core.connector.messagescalar.ScalarInput
import org.df4k.core.node.Action
import org.df4k.core.node.AsyncProcedure
import org.df4k.core.node.AsyncTask
import org.df4k.core.node.messagestream.Actor
import org.df4k.core.node.messagestream.PickPoint
import org.junit.Test

import java.util.Random
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import org.junit.Assert.assertTrue

/**
 * Demonstrates usage of class [PickPoint] to model common places for tokens.
 */
class DiningPhilosophers {

    //    @Ignore
    @Test
    @Throws(InterruptedException::class)
    fun test() {
        val forkPlaces = arrayOfNulls<ForkPlace>(num)
        val counter = CountDownLatch(num)
        val philosophers = arrayOfNulls<Philosopher>(num)
        // create places for forks with 1 fork in each
        for (k in 0 until num) {
            val forkPlace = ForkPlace(k)
            forkPlace.post(Fork(k))
            forkPlaces[k] = forkPlace
        }
        // create philosophers
        for (k in 0 until num) {
            philosophers[k] = Philosopher(k, forkPlaces, counter)
        }
        // animate all philosophers
        for (k in 0 until num) {
            philosophers[k].start()
        }
        assertTrue(counter.await(2000, TimeUnit.MILLISECONDS))
    }

    internal class Fork(id: Int) {
        val id: String

        init {
            this.id = "Fork_$id"
        }

        override fun toString(): String {
            return id
        }
    }

    internal class ForkPlace(var id: Int) : PickPoint<Fork>() {
        var label: String

        init {
            label = "Forkplace_$id"
        }

        override fun postFailure(t: Throwable) {
            super.postFailure(t)
        }

        override fun post(resource: Fork?) {
            println(label + ": put " + resource!!.toString())
            super.post(resource)
        }
    }

    /**
     * while ordinary [Actor] is a single [AsyncTask]
     * which restarts itself,
     * this class comprises of several [AsyncTask]s which activate each other cyclically.
     */
    internal class Philosopher(var id: Int, forkPlaces: Array<ForkPlace>, var counter: CountDownLatch) {
        var rand = Random()
        var firstPlace: ForkPlace
        var secondPlace: ForkPlace
        var first: Fork? = null
        var second: Fork
        var indent: String
        var rounds = 0

        var hungry = Hungry()
        var replete = Replete()
        var think: AsyncProcedure<*> = DelayedAsyncProc(hungry)
        var eat: AsyncProcedure<*> = DelayedAsyncProc(replete)

        init {
            // to avoid deadlocks, allocate resource with lower number first
            if (id == num - 1) {
                firstPlace = forkPlaces[0]
                secondPlace = forkPlaces[id]
            } else {
                firstPlace = forkPlaces[id]
                secondPlace = forkPlaces[id + 1]
            }

            val sb = StringBuffer()
            sb.append(id).append(":")
            for (k in 0..id) sb.append("  ")
            indent = sb.toString()
            println("first place (" + firstPlace.id + ") second place (" + secondPlace.id + ")")
        }

        fun start() {
            think.start()
        }

        private fun println(s: String) {
            println(indent + s)
        }

        private inner class DelayedAsyncProc private constructor(internal val next: AsyncProcedure<*>) : AsyncProcedure<Void>() {

            @Action
            @Throws(InterruptedException::class)
            protected fun act(): Void? {
                Thread.sleep(rand.nextLong() % 11 + 11)
                next.start()
                return null
            }
        }

        /**
         * collects forks one by one
         */
        private inner class Hungry : AsyncProcedure<Void>() {
            internal var input = ScalarInput<Fork>(this)

            override fun start() {
                println("Request first (" + firstPlace.id + ")")
                firstPlace.subscribe(this.input)
                super.start()
            }

            @Action
            protected fun act(fork: Fork) {
                if (first == null) {
                    first = fork
                    println("Request second (" + secondPlace.id + ")")
                    secondPlace.subscribe(this.input)
                    super.start()
                } else {
                    second = fork
                    eat.start()
                }
            }
        }

        /** return forks
         *
         */
        private inner class Replete : AsyncProcedure<Void>() {

            @Action
            protected fun act(): Void? {
                println("Release first (" + firstPlace.id + ")")
                firstPlace.post(first)
                println("Release second (" + secondPlace.id + ")")
                secondPlace.post(second)
                rounds++
                if (rounds < 10) {
                    think.start()
                } else {
                    counter.countDown()
                }
                return null
            }
        }
    }

    companion object {
        private val num = 5
    }
}
