package org.df4k.core.connector.reactivestream

import org.df4k.core.connector.messagescalar.SimpleSubscription

interface ReactiveSubscription : SimpleSubscription {

    fun request(n: Long)

}
