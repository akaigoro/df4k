package org.df4k.core.node.messagestream

import org.df4k.core.node.AsyncProcedure

/**
 * Actor is a reusable AsyncProc: after execution, it executes again as soon as new array of arguments is ready.
 */
open class Actor : AsyncProcedure<Void>() {
    override fun run() {
        try {
            controlLock.turnOff()
            runAction()
            start() // restart execution
        } catch (e: Throwable) {
            stop()
        }

    }

}
