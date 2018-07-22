package org.df4k.juc

import org.junit.BeforeClass
import org.junit.Test

import java.util.ArrayList
import java.util.Arrays
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

import java.util.Arrays.asList
import java.util.concurrent.CompletableFuture.supplyAsync
import org.junit.Assert.assertEquals
import org.junit.Assert.fail

/**
 * https://gist.github.com/spullara/5897605
 */
class CompletableFutureTest {

    @Test
    fun testExceptions() {

        val future = CompletableFuture<Any>()
        future.completeExceptionally(RuntimeException())
        future.exceptionally { t ->
            t.printStackTrace()
            throw CompletionException(t)
        }

        val future2 = supplyAsync<Any> { throw RuntimeException() }
        future2.exceptionally { t ->
            t.printStackTrace()
            throw CompletionException(t)
        }

        val future3 = supplyAsync { "test" }
        future3.thenAccept { t -> throw RuntimeException() }.exceptionally { t ->
            t.printStackTrace()
            throw CompletionException(t)
        }
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testCancellation() {
        val cancelled = AtomicBoolean()
        val handled = AtomicBoolean()
        val handleCalledWithValue = AtomicBoolean()
        val other = supplyAsync { "Doomed value" }
        val future = supplyAsync {
            sleep(1000)
            "Doomed value"
        }.exceptionally { t ->
            cancelled.set(true)
            null
        }.thenCombine(other) { a, b -> "$a, $b" }.handle<String> { v, t ->
            if (t == null) {
                handleCalledWithValue.set(true)
            }
            handled.set(true)
            null
        }
        sleep(100)
        future.cancel(true)
        sleep(1000)
        try {
            future.get()
            fail("Should have thrown")
        } catch (ce: CancellationException) {
            println("future cancelled: " + future.isCancelled)
            println("other cancelled: " + other.isCancelled)
            println("exceptionally called: " + cancelled.get())
            println("handle called: " + handled.get())
            println("handle called with value: " + handleCalledWithValue.get())
        }

    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testCompleteExceptionally() {
        val cancelled = AtomicBoolean()
        val handled = AtomicBoolean()
        val handleCalledWithValue = AtomicBoolean()
        val other = supplyAsync { "Doomed value" }
        val future = supplyAsync {
            sleep(1000)
            "Doomed value"
        }.exceptionally { t ->
            cancelled.set(true)
            null
        }.thenCombine(other) { a, b -> "$a, $b" }.handle<String> { v, t ->
            if (t == null) {
                handleCalledWithValue.set(true)
            }
            handled.set(true)
            null
        }
        sleep(100)
        future.completeExceptionally(CancellationException())
        sleep(1000)
        try {
            future.get()
            fail("Should have thrown")
        } catch (ce: CancellationException) {
            println("future cancelled: " + future.isCancelled)
            println("other cancelled: " + other.isCancelled)
            println("exceptionally called: " + cancelled.get())
            println("handle called: " + handled.get())
            println("handle called with value: " + handleCalledWithValue.get())
        }

    }

    @Test
    fun testExceptionally() {
        val called = AtomicBoolean()
        val future = CompletableFuture<Any>().exceptionally { t ->
            called.set(true)
            null
        }
        future.completeExceptionally(CancellationException())
        try {
            future.get()
        } catch (e: Exception) {
            println("exceptionally called: $called")
        }

    }

    @Test
    @Throws(Exception::class)
    fun testCompletableFutures() {
        val executed = AtomicBoolean(false)
        val future = supplyAsync {
            sleep(1000)
            "Done."
        }
        val future1 = supplyAsync {
            sleep(900)
            "Done2."
        }
        val future2 = supplyAsync { "Constant" }
        val future3 = supplyAsync<String> {
            sleep(500)
            throw RuntimeException("CompletableFuture4")
        }
        val future4 = CompletableFuture<String>()
        future4.completeExceptionally(RuntimeException("CompletableFuture5"))
        val future5 = supplyAsync {
            executed.set(true)
            sleep(1000)
            "Done."
        }
        future5.cancel(true)

        val selected = select(future, future1, future3, future4)

        /* TODO FIX
        try {
            junit.framework.Assert.assertTrue(future5.isCancelled());
            junit.framework.Assert.assertTrue(future5.isDone());
            future5.get();
            fail("Was not cancelled");
        } catch (CancellationException ce) {
            if (executed.get()) {
                fail("Executed though cancelled immediately");
            }
        }
*/
        val result10 = CompletableFuture<String>()
        try {
            onFailure(future3, { e -> result10.complete("Failed") }).get(0, TimeUnit.SECONDS)
            fail("Didn't timeout")
        } catch (te: TimeoutException) {
        }

        try {
            future4.thenApply<Any> { v -> null }.get()
            fail("Didn't fail")
        } catch (ee: ExecutionException) {
        }

        val result3 = CompletableFuture<String>()
        future.applyToEither(future1) { v -> result3.complete("Selected: $v") }
        val result4 = CompletableFuture<String>()
        future1.applyToEither(future) { v -> result4.complete("Selected: $v") }
        assertEquals("Selected: Done2.", result3.get())
        assertEquals("Selected: Done2.", result4.get())
        assertEquals("Done2.", selected.get())

        val map1 = future.thenCombine(future1) { value1, value2 -> "$value1, $value2" }
        val map2 = future1.thenCombine(future) { value1, value2 -> "$value1, $value2" }
        assertEquals("Done., Done2.", map1.get())
        assertEquals("Done2., Done.", map2.get())

        val result1 = CompletableFuture<String>()
        future.acceptEither(future3) { s -> result1.complete("Selected: $s") }
        assertEquals("Selected: Done.", result1.get())
        assertEquals("Failed", result10.get())

        try {
            onFailure(future3.acceptEither(future4) { e -> }, { e -> result1.complete(e.message) }).get()
            fail("Didn't fail")
        } catch (ee: ExecutionException) {
        }

        //        final CountDownLatch monitor = new CountDownLatch(2);
        //        CompletableFuture<String> onraise = supplyAsync(() -> {
        //            try {
        //                monitor.await();
        //            } catch (InterruptedException e) {
        //            }
        //            return "Interrupted";
        //        });
        //        CompletableFuture<String> join = future2.thenCombine(onraise, (a, b) -> null);
        //        onraise.onRaise(e -> monitor.countDown());
        //        onraise.onRaise(e -> monitor.countDown());

        //        CompletableFuture<String> map = future.map(v -> "Set1: " + v).map(v -> {
        //            join.raise(new CancellationException());
        //            return "Set2: " + v;
        //        });

        //        assertEquals("Set2: Set1: Done.", map.get());
        //        assertEquals(new Pair<>("Constant", "Interrupted"), join.get());
        //
        try {
            future.thenCombine(future3) { value1, value2 -> "$value1, $value2" }.get()
            fail("Didn't fail")
        } catch (ee: ExecutionException) {
        }

        assertEquals("Flatmapped: Constant", future1.thenCompose { v -> future2 }.thenApply { v -> "Flatmapped: $v" }.get())

        val result11 = CompletableFuture<String>()
        try {
            onFailure(future1.thenApply { v -> future3 }, { e -> result11.complete("Failed") }).get()
        } catch (ee: ExecutionException) {
            assertEquals("Failed", result11.get())
        }

        val result2 = CompletableFuture<String>()
        onFailure(future3.thenCompose { v -> future1 }, { e -> result2.complete("Flat map failed: $e") })
        assertEquals("Flat map failed: java.util.concurrent.CompletionException: java.lang.RuntimeException: CompletableFuture4", result2.get())

        assertEquals("Done.", future.get(1, TimeUnit.DAYS))

        try {
            future3.get()
            fail("Didn't fail")
        } catch (e: ExecutionException) {
        }

        try {
            future3.thenCombine<String, Any>(future) { a, b -> null }.get()
            fail("Didn't fail")
        } catch (e: ExecutionException) {
        }

        val result5 = CompletableFuture<String>()
        val result6 = CompletableFuture<String>()
        onFailure(future.thenAccept { s -> result5.complete("onSuccess: $s") },
                { e -> result5.complete("onFailure: $e") })
                .thenRun { result6.complete("Ensured") }
        assertEquals("onSuccess: Done.", result5.get())
        assertEquals("Ensured", result6.get())

        val result7 = CompletableFuture<String>()
        val result8 = CompletableFuture<String>()
        ensure(onFailure(future3.thenAccept { s -> result7.complete("onSuccess: $s") }, { e -> result7.complete("onFailure: $e") }), { result8.complete("Ensured") })
        assertEquals("onFailure: java.util.concurrent.CompletionException: java.lang.RuntimeException: CompletableFuture4", result7.get())
        assertEquals("Ensured", result8.get())

        assertEquals("Was Rescued!", future3.exceptionally { e -> "Rescued!" }.thenApply { v -> "Was $v" }.get())
        assertEquals("Was Constant", future2.exceptionally { e -> "Rescued!" }.thenApply { v -> "Was $v" }.get())

        assertEquals(asList("Done.", "Done2.", "Constant"), collect(asList(future, future1, future2)).get())
        assertEquals(Arrays.asList<String>(), collect(ArrayList()).get())
        try {
            assertEquals(asList("Done.", "Done2.", "Constant"), collect(asList(future, future3, future2)).get())
            fail("Didn't fail")
        } catch (ee: ExecutionException) {
        }

        val result9 = CompletableFuture<String>()
        future.thenAccept { v -> result9.complete("onSuccess: $v") }
        assertEquals("onSuccess: Done.", result9.get())
    }

    private fun collect(completableFutures: List<CompletableFuture<String>>): CompletableFuture<List<String>> {
        val result = CompletableFuture<List<String>>()
        val size = completableFutures.size
        val list = ArrayList<String>()
        if (size == 0) {
            result.complete(list)
        } else {
            for (completableFuture in completableFutures) {
                completableFuture.handle { s, t ->
                    if (t == null) {
                        list.add(s)
                        if (list.size == size) {
                            result.complete(list)
                        }
                    } else {
                        result.completeExceptionally(t)
                    }
                    s
                }
            }
        }
        return result
    }

    @SafeVarargs
    private fun select(vararg completableFutures: CompletableFuture<String>): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        for (completableFuture in completableFutures) {
            completableFuture.thenAccept { future.complete(it) }
        }
        return future
    }

    private fun sleep(millis: Int) {
        try {
            Thread.sleep(millis.toLong())
        } catch (e: InterruptedException) {
        }

    }

    internal fun run(run: Runnable) {
        run.run()
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val pt = CompletableFutureTest()
            setup()
            pt.testCompletableFutures()
            println("Success.")
        }

        @BeforeClass
        fun setup() {
        }

        private fun <T> onFailure(future: CompletableFuture<T>, call: Consumer<Throwable>): CompletableFuture<T> {
            val completableFuture = CompletableFuture<T>()
            future.handle<Any> { v, t ->
                if (t == null) {
                    completableFuture.complete(v)
                } else {
                    call.accept(t)
                    completableFuture.completeExceptionally(t)
                }
                null
            }
            return completableFuture
        }

        private fun <T> ensure(future: CompletableFuture<T>, call: Runnable): CompletableFuture<T> {
            val completableFuture = CompletableFuture<T>()
            future.handle<Any> { v, t ->
                if (t == null) {
                    call.run()
                    completableFuture.complete(v)
                } else {
                    call.run()
                    completableFuture.completeExceptionally(t)
                }
                null
            }
            return completableFuture
        }
    }
}