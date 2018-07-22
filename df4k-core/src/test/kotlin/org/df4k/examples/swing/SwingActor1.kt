package org.df4k.examples.swing

import org.df4k.core.node.messagestream.Actor1

import java.awt.EventQueue

abstract class SwingActor1<T> : Actor1<T>() {
    init {
        setExecutor(???({ EventQueue.invokeLater(it) }))
        start()
    }
}
