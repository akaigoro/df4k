package org.df4k.core.util

import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.Executor

class DirectExecutor : Executor {

    override fun execute(command: Runnable) {
        command.run()
    }

    companion object {
        val directExecutor = DirectExecutor()
    }
}
