package org.df4k.core.util

import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.Executor

class SameThreadExecutor : Executor {

    override fun execute(command: Runnable) {
        var command = command
        var queue: Queue<Runnable>? = myThreadLocal.get()
        if (queue == null) {
            queue = ArrayDeque()
            myThreadLocal.set(queue)
            command.run()
            while ((command = queue.poll()) != null) {
                try {
                    command.run()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

            }
            myThreadLocal.remove()
        } else {
            queue.add(command)
        }
    }

    companion object {
        private val myThreadLocal = ThreadLocal<Queue<Runnable>>()
        val sameThreadExecutor = SameThreadExecutor()
    }
}
