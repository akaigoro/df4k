/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4k.core.node

import org.df4k.core.util.DirectExecutor
import org.df4k.core.util.SameThreadExecutor

import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicInteger

/**
 * AsyncProc is an Asynchronous Procedure Call.
 *
 * It consists of asynchronous connectors, implemented as inner classes,
 * user-defined asynchronous procedure, and a mechanism to call that procedure
 * using supplied [java.util.concurrent.Executor] as soon as all connectors are unblocked.
 *
 * This class contains base classes for locks and connectors
 */
abstract class AsyncTask : Runnable {

    /**
     * the set of all b/w Pins
     */
    protected val locks = HashSet<Lock>()
    /**
     * the set of all colored Pins, to form array of arguments
     */
    protected val asynctParams = ArrayList<AsynctParam<*>>()
    /**
     * total number of created pins
     */
    protected var pinCount = AtomicInteger()
    /**
     * total number of created pins
     */
    protected var blockedPinCount = AtomicInteger()

    protected var executor: Executor = ForkJoinPool.commonPool()

    protected abstract val isStarted: Boolean

    /**
     * assigns Executor
     */
    fun setExecutor(exec: Executor?) {
        if (exec == null) {
            this.executor = SameThreadExecutor.sameThreadExecutor
        } else {
            this.executor = exec
        }
    }

    fun getExecutor(): Executor {
        return executor
    }

    /**
     * invoked when all asyncTask asyncTask are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected open fun fire() {
        getExecutor().execute(this)
    }

    /**
     * Basic class for all locs and connectors (places for tokens).
     * Asynchronous version of binary semaphore.
     *
     *
     * initially in non-blocked state
     */
    private abstract inner class BaseLock @JvmOverloads constructor(blocked: Boolean = true) {
        internal var pinNumber: Int = 0 // distinct for all other connectors of this node
        var isBlocked: Boolean = false
            internal set

        init {
            this.pinNumber = pinCount.getAndIncrement()
            this.isBlocked = blocked
            if (blocked) {
                blockedPinCount.incrementAndGet()
            }
            register()
        }

        /**
         * locks the pin
         * called when a token is consumed and the pin become empty
         */
        fun turnOff() {
            if (isBlocked) {
                return
            }
            isBlocked = true
            blockedPinCount.incrementAndGet()
        }

        fun turnOn() {
            if (!isBlocked) {
                return
            }
            isBlocked = false
            val res = blockedPinCount.decrementAndGet().toLong()
            if (res == 0L) {
                fire()
            }
        }

        protected abstract fun register()

        protected abstract fun unRegister()
    }
    /**
     * by default, initially in blocked state
     */


    /**
     * Basic class for all permission parameters (places for black/white tokens).
     * Asynchronous version of binary semaphore.
     *
     *
     * initially in non-blocked state
     */
    open inner class Lock : BaseLock {

        constructor(blocked: Boolean) : super(blocked) {}

        constructor() : super() {}

        override fun register() {
            locks.add(this)
        }

        override fun unRegister() {
            if (isBlocked) {
                turnOn()
            }
            locks.remove(this)
        }

        /**
         * Executed after token processing (method act). Cleans reference to
         * value, if any. Signals to set state to off if no more tokens are in
         * the place. Should return quickly, as is called from the actor's
         * synchronized block.
         */
        open fun purge() {}
    }

    /**
     * Basic class for all valued parameters (places for colored tokens).
     *
     *
     * initially in blocked state
     */
    abstract inner class AsynctParam<T> : BaseLock {

        constructor(blocked: Boolean) : super(blocked) {}

        constructor() : super() {}

        override fun register() {
            if (isStarted) {
                throw IllegalStateException("cannot register connector after start")
            }
            asynctParams.add(this)
        }

        override fun unRegister() {
            if (isStarted) {
                throw IllegalStateException("cannot unregister connector after start")
            }
            if (isBlocked) {
                turnOn()
            }
            asynctParams.remove(this)
        }

        /** removes and return next token  */
        abstract operator fun next(): T
    }

    companion object {
        val directExecutor = DirectExecutor.directExecutor
        val sameThreadExecutor = SameThreadExecutor.sameThreadExecutor
    }
}