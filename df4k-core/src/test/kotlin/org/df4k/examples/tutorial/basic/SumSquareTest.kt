package org.df4k.examples.tutorial.basic

import org.df4k.core.connector.messagescalar.CompletablePromise
import org.df4k.core.connector.messagescalar.ScalarInput
import org.df4k.core.node.Action
import org.df4k.core.node.AsyncProcedure
import org.df4k.core.node.messagescalar.AsyncBiFunction
import org.df4k.core.node.messagescalar.AsyncFunction
import org.junit.Assert
import org.junit.Test

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.function.BiFunction
import java.util.function.Function

class SumSquareTest {
    class Square : AsyncProcedure<*>() {
        internal val result = CompletablePromise<Int>()
        internal val param = ScalarInput<Int>(this)

        @Action
        fun compute(arg: Int?) {
            val res = arg!! * arg
            result.complete(res)
        }
    }

    class Sum : AsyncProcedure<*>() {
        internal val result = CompletablePromise<Int>()
        internal val paramX = ScalarInput<Int>(this)
        internal val paramY = ScalarInput<Int>(this)

        @Action
        fun compute(argX: Int?, argY: Int?) {
            val res = argX!! + argY!!
            result.complete(res)
        }
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testAP() {
        // create 3 nodes
        val sqX = Square()
        val sqY = Square()
        val sum = Sum()
        // make 2 connections
        sqX.result.subscribe(sum.paramX)
        sqY.result.subscribe(sum.paramY)
        // start all the nodes
        sqX.start()
        sqY.start()
        sum.start()
        // provide input information:
        sqX.param.post(3)
        sqY.param.post(4)
        // get the result
        val res = sum.result.get()
        Assert.assertEquals(25, res.toLong())
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testDFF() {
        val square = { arg -> arg!! * arg!! }
        val plus = { argX, argY -> argX!! + argY!! }
        // create nodes and connect them
        val sqX = AsyncFunction<Int, Int>(square)
        val sqY = AsyncFunction<Int, Int>(square)
        val sum = AsyncBiFunction<Int, Int, Int>(plus)
        // make 2 connections
        sqX.subscribe(sum.param1)
        sqY.subscribe(sum.param2)
        // start all the nodes
        sqX.start()
        sqY.start()
        sum.start()
        // provide input information:
        sqX.post(3)
        sqY.post(4)
        // get the result
        val res = sum.asyncResult().get()
        Assert.assertEquals(25, res.toLong())
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testCF() {
        val square = { arg -> arg!! * arg!! }
        val plus = { argX, argY -> argX!! + argY!! }
        // create nodes and connect them
        val sqXParam = CompletableFuture()
        val sqYParam = CompletableFuture()
        val sum = sqXParam
                .thenApply(square)
                .thenCombine(sqYParam.thenApply(square),
                        plus)
        // provide input information:
        sqXParam.complete(3)
        sqYParam.complete(4)
        // get the result
        val res = sum.get()
        Assert.assertEquals(25, res.toLong())
    }

}
