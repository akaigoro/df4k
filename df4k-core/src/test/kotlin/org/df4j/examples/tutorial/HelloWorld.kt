package org.df4j.examples.tutorial

import org.df4j.core.connector.messagestream.StreamInput
import org.df4j.core.node.Action
import org.df4j.core.node.messagestream.Actor
import org.junit.Test

class HelloWorld {
    /**
     * collects strings
     * prints collected strings when argument is an empty string
     */
    internal inner class Collector : Actor() {

        var input = StreamInput<String>(this) // actor's parameter
        var sb = StringBuilder()

        @Action
        protected fun act(message: String?) {
            // empty string indicates the end of stream
            // nulls are not allowed
            if (message == null) {
                println(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(message)
                sb.append(" ")
            }
        }
    }

    @Test
    fun test() {
        val coll = Collector()
        coll.start()
        coll.input.post("Hello")
        coll.input.post("World")
        coll.input.complete()
    }

}
