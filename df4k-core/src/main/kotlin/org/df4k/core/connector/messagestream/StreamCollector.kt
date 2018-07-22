package org.df4k.core.connector.messagestream

import org.df4k.core.connector.messagescalar.ScalarCollector

interface StreamCollector<T> : ScalarCollector<T> {

    /** closes the message stream  */
    fun complete()
}
