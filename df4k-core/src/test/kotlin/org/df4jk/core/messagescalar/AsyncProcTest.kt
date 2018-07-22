package org.df4jk.core.messagescalar

import org.df4j.core.connector.messagescalar.CompletablePromise
import org.df4j.core.connector.messagescalar.CompletedPromise
import org.df4j.core.connector.messagescalar.ConstInput
import org.df4j.core.connector.messagescalar.ScalarPublisher
import org.df4j.core.node.Action
import org.df4j.core.node.messagescalar.AsyncBiFunction
import org.df4j.core.node.messagescalar.AsyncSupplier
import org.junit.Assert
import org.junit.Test

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class AsyncProcTest {

    // smoke test
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun computeMult(a: Double, b: Double, expected: Double) {
        val mult = Mult()
        mult.param1.post(a)
        mult.param2.post(b)
        val result = mult.asyncResult().get(10000, TimeUnit.SECONDS)
        Assert.assertEquals(expected, result, 0.001)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun runMultTest() {
        computeMult(3.0, 4.0, 12.0)
        computeMult(-1.0, -2.0, 2.0)
    }

    class Blocker<T, R> : AsyncSupplier<R>() {
        internal var arg = ConstInput<T>(this)
    }

    internal inner class Mult2 : Mult() {
        var pa = CompletablePromise<Double>()
        var pb = CompletablePromise<Double>()

        init {
            val sp = CompletablePromise<Double>()
            val blocker = Blocker<Double, Double>()
            pa.subscribe(blocker.arg)
            CompletedPromise(1.0).subscribe(param1)
            Mult(pa, pb).subscribe(param2)
        }
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun computeMult2(a: Double, b: Double, expected: Double) {
        val mult = Mult2()
        mult.pa.complete(a)
        mult.pb.complete(b)
        val result = mult.asFuture().get(1, TimeUnit.SECONDS)
        Assert.assertEquals(expected, result, 0.001)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun runMultTest2() {
        computeMult2(3.0, 4.0, 12.0)
        computeMult2(-1.0, -2.0, 2.0)
    }

    /* D = b^2 - 4ac */
    internal inner class Discr : AsyncSupplier<Double>() {
        var pa = ConstInput<Double>(this)
        var pb = ConstInput<Double>(this)
        var pc = ConstInput<Double>(this)

        @Action
        fun act(a: Double?, b: Double?, c: Double?): Double {
            return b!! * b - 4.0 * a!! * c!!
        }
    }

    private fun computeDiscr(a: Double, b: Double, c: Double): CompletablePromise<Double> {
        val d = Discr()
        d.pa.post(a)
        d.pb.post(b)
        d.pc.post(c)
        d.start()
        return d.asyncResult()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun computeDiscrAndScheck(a: Double, b: Double, c: Double, expected: Double) {
        val asyncResult = computeDiscr(a, b, c)
        val result = asyncResult.get(1, TimeUnit.SECONDS)
        Assert.assertEquals(expected, result!!, 0.001)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun runDiscrTest() {
        computeDiscrAndScheck(3.0, -4.0, 1.0, 4.0)
        computeDiscrAndScheck(1.0, 4.0, 4.0, 0.0)
        computeDiscrAndScheck(2.0, 6.0, 5.0, -4.0)
    }

    /**
     * (-b +/- sqrt(D))/2a
     */
    internal inner class RootCalc : AsyncSupplier<DoubleArray>() {
        var pa = ConstInput<Double>(this)
        var pb = ConstInput<Double>(this)
        var pd = ConstInput<Double>(this)

        @Action
        fun act(a: Double, b: Double, d: Double): DoubleArray {
            if (d < 0) {
                return DoubleArray(0)
            } else {
                val sqrt_d = Math.sqrt(d!!)
                val root1 = ((-b)!! - sqrt_d) / (2 * a!!)
                val root2 = ((-b)!! + sqrt_d) / (2 * a)
                return doubleArrayOf(root1, root2)
            }
        }
    }

    private fun calcRoots(a: Double, b: Double, d: ScalarPublisher<Double>): CompletablePromise<DoubleArray> {
        val rc = RootCalc()
        rc.pa.post(a)
        rc.pb.post(b)
        d.subscribe(rc.pd)
        rc.start()
        return rc.asyncResult()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun calcRootsAndCheck(a: Double, b: Double, d: Double, vararg expected: Double) {
        val rc = calcRoots(a, b, CompletedPromise(d))
        val result = rc.get(1, TimeUnit.SECONDS)
        Assert.assertArrayEquals(expected, result, 0.001)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun calcRootsTest() {
        calcRootsAndCheck(1.0, -4.0, 4.0, 1.0, 3.0)
        calcRootsAndCheck(1.0, 4.0, 0.0, -2.0, -2.0)
        calcRootsAndCheck(1.0, 6.0, -4.0)
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun computeRoots(a: Double, b: Double, c: Double, vararg expected: Double) {
        val d = computeDiscr(a, b, c)
        val rc = calcRoots(a, b, d)
        val result = rc.get(1, TimeUnit.SECONDS)
        Assert.assertArrayEquals(expected, result, 0.001)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun equationTest() {
        computeRoots(3.0, -4.0, 1.0, 0.333, 1.0)
        computeRoots(1.0, 4.0, 4.0, -2.0, -2.0)
        computeRoots(1.0, 6.0, 45.0)
    }

    internal open class Mult() : AsyncBiFunction<Double, Double, Double>(fun(val1: Double, val2: Double):Double{return val1 * val2 }) {

        init {
            start()
        }

        constructor(pa: ScalarPublisher<Double>, pb: ScalarPublisher<Double>) : this() {
            pa.subscribe(param1)
            pb.subscribe(param2)
        }
    }
}
