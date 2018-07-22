package org.df4j.examples.swing

import org.df4j.core.node.messagestream.Actor1

import java.awt.*
import java.util.concurrent.Executor

abstract class SwingActor1<T> : Actor1<T>() {
    init {
        setExecutor(Executor { EventQueue.invokeLater(it) })
        start()
    }
}
