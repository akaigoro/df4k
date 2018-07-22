/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4jk.examples.swing

import org.df4j.core.node.Action
import org.df4j.core.node.messagestream.Actor1

import javax.swing.*
import javax.swing.border.LineBorder
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowEvent

/**
 * Interaction between GUI and Actors.
 * EDT (JTextField) -> Executor (computing actor) -> EDT (printing actor)
 */
class SwingActorTest : JFrame() {
    internal var jTextField = JTextField()
    internal var jlist = JTextArea()
    internal var jLabel2 = JLabel()
    internal var workCount: Int = 0

    internal var ca = ComputingActor()
    internal var pa = PrintingActor()

    init {
        this.title = "SwingActor Test"
        this.setSize(360, 300)
        this.contentPane.layout = null

        val jLabel = JLabel()
        jLabel.setBounds(24, 40, 120, 18)
        jLabel.text = "Enter number:"
        this.add(jLabel, null)

        jTextField.setBounds(162, 40, 120, 20)
        jTextField.addActionListener(ca)
        this.add(jTextField, null)

        jLabel2.setBounds(34, 80, 80, 20)
        this.add(jLabel2, null)

        jlist.setBounds(34, 120, 200, 120)
        jlist.border = LineBorder(Color.BLACK)
        this.add(jlist, null)

        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(winEvt: WindowEvent?) {
                System.exit(0)
            }
        })
    }

    internal inner class ComputingActor : Actor1<String>(), ActionListener {
        init {
            start()
        }

        /**
         * handles messages on EDT.
         */
        override fun actionPerformed(e: ActionEvent) {
            // GUI (JTextField) -> computing actor
            workCount++
            jLabel2.text = "working..."
            this.post(jTextField.text)
        }

        /**
         * Processes message asynchronously out of EDT.
         */
        @Action
        @Throws(Exception::class)
        protected fun act(str: String) {
            var str = str
            Thread.sleep(2000) // imitate working hard
            try {
                val num = Integer.valueOf(str)
                str = num.toString() + " is integer\n"
            } catch (e: NumberFormatException) {
                try {
                    val num = java.lang.Double.valueOf(str)
                    str = num.toString() + " is double\n"
                } catch (e2: NumberFormatException) {
                    str = "$str is not a number\n"
                }

            }

            // computing actor -> GUI (printing actor)
            pa.post(str)
        }
    }

    /**
     * Processes messages on EDT.
     */
    internal inner class PrintingActor : SwingActor1<String>() {

        @Action
        @Throws(Exception::class)
        protected fun act(m: String) {
            jlist.append(m)
            workCount--
            if (workCount == 0) {
                jLabel2.text = ""
            }
        }
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            EventQueue.invokeLater { SwingActorTest().isVisible = true }
        }
    }

}
