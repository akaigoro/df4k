package org.df4k.core.node

import org.df4k.core.util.ActionCaller
import org.df4k.core.util.invoker.Invoker

import java.util.concurrent.Executor

/**
 * this class contains components, likely useful in each async node:
 * - control pin
 * - action caller
 *
 * @param <R>
</R> */
open class AsyncProcedure<R> : AsyncTask {

    protected var actionCaller: Invoker<R>? = null
    @Volatile
    override var isStarted = false
        protected set
    @Volatile
    protected var stopped = false

    /**
     * blocked initially, until [.start] called.
     * blocked when this actor goes to executor, to ensure serial execution of the act() method.
     */
    protected var controlLock = AsyncTask.Lock()

    constructor() {}

    constructor(actionCaller: Invoker<R>) {
        this.actionCaller = actionCaller
    }

    @Synchronized
    open fun start() {
        if (stopped) {
            return
        }
        isStarted = true
        controlLock.turnOn()
    }

    @Synchronized
    fun start(executor: Executor) {
        setExecutor(executor)
        start()
    }

    @Synchronized
    fun stop() {
        stopped = true
        controlLock.turnOff()
    }

    @Synchronized
    fun consumeTokens(): Array<Any> {
        if (!isStarted) {
            throw IllegalStateException("not started")
        }
        locks.forEach { lock -> lock.purge() }
        val args = arrayOfNulls<Any>(asynctParams.size)
        for (k in asynctParams.indices) {
            val asynctParam = asynctParams[k]
            args[k] = asynctParam.next()
        }
        return args
    }

    @Throws(Exception::class)
    protected open fun runAction(): R {
        if (actionCaller == null) {
            try {
                actionCaller = ActionCaller.findAction(this, asynctParams.size)
            } catch (e: NoSuchMethodException) {
                throw IllegalStateException(e)
            }

        }
        val args = consumeTokens()
        return actionCaller!!.apply(*args)
    }

    override fun run() {
        try {
            controlLock.turnOff()
            runAction()
        } catch (e: Throwable) {
            stop()
        }

    }
}
