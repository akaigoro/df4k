package org.df4k.core.util

import java.util.logging.ConsoleHandler
import java.util.logging.Level

class Logger protected constructor(name: String) : java.util.logging.Logger(name, null) {
    companion object {

        override fun getLogger(name: String): Logger {
            val logger = Logger(name)
            logger.level = Level.FINE
            logger.addHandler(ConsoleHandler())
            return logger
        }
    }
}
