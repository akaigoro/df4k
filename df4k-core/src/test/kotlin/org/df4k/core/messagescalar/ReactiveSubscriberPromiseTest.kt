package org.df4k.core.messagescalar

import org.df4k.core.connector.messagescalar.SubscriberPromise
import org.junit.Assert
import org.junit.Test

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ReactiveSubscriberPromiseTest {

    @Test
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun singleSubscriberTest() {
        val sp0 = SubscriberPromise<Double>()
        val sp1 = SubscriberPromise<Double>()
        val sp2 = SubscriberPromise<Double>()
        sp0.subscribe(sp1)
        sp0.subscribe(sp2)
        val v = 4.0
        sp0.post(v)
        val val1 = sp1.asFuture().get(1, TimeUnit.SECONDS).toDouble()
        Assert.assertEquals(v, val1, 0.0001)
        val val2 = sp2.asFuture().get(1, TimeUnit.SECONDS).toDouble()
        Assert.assertEquals(v, val2, 0.0001)
    }
}
