package org.df4k.examples.tutorial

import org.df4k.core.node.Action
import org.df4k.core.node.messagestream.Actor1
import org.junit.Test

class HelloWorld1 {
    /**
     * collects strings
     * prints collected strings when argument is an empty string
     */
    internal inner class Collector : Actor1<String>() {
        var sb = StringBuilder()

        @Action
        protected fun act(message: String) {
            sb.append(message)
            sb.append(" ")
        }

        protected fun onCompleted() {
            println(sb.toString())
            sb.setLength(0)
        }

    }

    @Test
    fun test() {
        val coll = Collector()
        coll.start()
        coll.post("Hello")
        coll.post("World")
        coll.complete()
    }

}
