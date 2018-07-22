package org.df4k.core.connector.messagescalar

import org.df4k.core.node.messagescalar.*
import org.df4k.core.util.SameThreadExecutor

import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.*
import java.util.function.*

/**
 * an unblocking single-shot output connector
 *
 * @param <T>
</T> */
open class CompletablePromise<T> : ScalarSubscriber<T>, ScalarPublisher<T>, Future<T> {

    protected var subscription: SimpleSubscription? = null
    /** place for demands  */
    private var requests: Queue<ScalarSubscriber<in T>>? = ArrayDeque()
    protected var cancelled = false
    protected var completed = false
    protected var result: T? = null
    protected var exception: Throwable? = null


    /**
     * Returns `true` if this AsyncFunc completed
     * exceptionally, in any way. Possible causes include
     * cancellation, explicit invocation of `completeExceptionally`, and abrupt termination of a
     * AsyncFunc action.
     *
     * @return `true` if this AsyncFunc completed
     * exceptionally
     */
    val isCompletedExceptionally: Boolean
        get() = exception != null


    /**
     * Returns the estimated number of CompletableFutures whose
     * completions are awaiting completion of this AsyncFunc.
     * This method is designed for use in monitoring system state, not
     * for synchronization control.
     *
     * @return the number of dependent CompletableFutures
     */
    val numberOfDependents: Int
        @Synchronized get() = if (requests == null) {
            0
        } else requests!!.size

    override fun post(message: T?) {
        complete(message)
    }

    override fun postFailure(ex: Throwable) {
        completeExceptionally(ex)
    }

    override fun onSubscribe(subscription: SimpleSubscription) {
        if (cancelled) {
            throw IllegalStateException("cancelled already")
        }
        if (completed) {
            throw IllegalStateException("completed already")
        }
        this.subscription = subscription
    }


    @Synchronized
    override fun <S : ScalarSubscriber<in T>> subscribe(subscriber: S): S {
        if (completed) {
            subscriber.post(result)
        } else if (exception != null) {
            subscriber.postFailure(exception!!)
        } else {
            requests!!.add(subscriber)
        }
        return subscriber
    }

    @Synchronized
    fun complete(result: T?): Boolean {
        if (isDone) {
            return false
        }
        this.result = result
        this.completed = true
        notifyAll()
        for (subscriber in requests!!) {
            subscriber.post(result)
        }
        requests = null
        return true
    }

    @Synchronized
    fun completeExceptionally(exception: Throwable?): Boolean {
        if (exception == null) {
            throw IllegalArgumentException("CompletablePromise::completeExceptionally(): argument may not be null")
        }
        if (isDone) {
            return false
        }
        this.exception = exception
        for (subscriber in requests!!) {
            subscriber.postFailure(exception)
        }
        requests = null
        return true
    }

    /**
     * wrong API design. Future is not a task.
     * @param mayInterruptIfRunning
     * @return
     */
    @Synchronized
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        cancelled = true
        if (subscription != null) {
            subscription!!.cancel()
        }
        return completeExceptionally(CancellationException())
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun isDone(): Boolean {
        return completed || exception != null
    }

    @Synchronized
    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): T {
        while (true) {
            if (result != null) {
                return result
            } else if (exception != null) {
                return throwStoredException()
            } else {
                wait()
            }
        }
    }

    @Throws(ExecutionException::class)
    private fun throwStoredException(): T {
        var x = exception
        val cause: Throwable
        if (x is CancellationException)
            throw x
        if (x is CompletionException && (cause = x.cause) != null)
            x = cause
        throw ExecutionException(x)
    }

    @Synchronized
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun get(timeout: Long, unit: TimeUnit): T? {
        val end = System.currentTimeMillis() + unit.toMillis(timeout)
        while (true) {
            if (completed) {
                return result
            } else if (exception != null) {
                throwStoredException()
            } else {
                val timeout1 = end - System.currentTimeMillis()
                if (timeout1 <= 0) {
                    throw TimeoutException()
                }
                wait(timeout1)
            }
        }
    }

    fun <U> thenApply(fn: Function<in T, out U>): CompletablePromise<U> {
        return thenApplyAsync(fn, syncExec)
    }

    fun <U> thenApplyAsync(
            fn: Function<in T, out U>): CompletablePromise<U> {
        return thenApplyAsync(fn, asyncExec)
    }

    fun <U> thenApplyAsync(
            fn: Function<in T, out U>, executor: Executor): CompletablePromise<U> {
        val asyncFunc = AsyncFunction<in T, out U>(fn)
        this.subscribe<AsyncFunction<in T, out U>>(asyncFunc)
        asyncFunc.start(executor)
        return asyncFunc.asyncResult()
    }

    fun thenAccept(action: Consumer<in T>): CompletablePromise<Void> {
        return thenAcceptAsync(action, syncExec)
    }

    @JvmOverloads
    fun thenAcceptAsync(action: Consumer<in T>,
                        executor: Executor = asyncExec): CompletablePromise<Void> {
        val asyncConsumer = AsyncFunction<T, Void>(action)
        this.subscribe(asyncConsumer)
        asyncConsumer.start(executor)
        return asyncConsumer.asyncResult()
    }

    fun thenRun(action: Runnable): CompletablePromise<Void> {
        return thenRunAsync(action, syncExec)
    }

    @JvmOverloads
    fun thenRunAsync(action: Runnable,
                     executor: Executor = asyncExec): CompletablePromise<Void> {
        val asyncTask = AsyncFunction<T, Void>(action)
        this.subscribe(asyncTask)
        asyncTask.start(executor)
        return asyncTask.asyncResult()
    }

    fun <U, V> thenCombine(other: CompletablePromise<out U>,
                           fn: BiFunction<in T, in U, out V>
    ): CompletablePromise<V> {
        return thenCombineAsync(other, fn, syncExec)
    }

    fun <U, V> thenCombineAsync(other: CompletablePromise<out U>,
                                fn: BiFunction<in T, in U, out V>
    ): CompletablePromise<V> {
        return thenCombineAsync(other, fn, asyncExec)
    }

    fun <U, V> thenCombineAsync(other: CompletablePromise<out U>,
                                fn: BiFunction<in T, in U, out V>,
                                executor: Executor
    ): CompletablePromise<V> {
        val asyncBiFunc = AsyncBiFunction(fn)
        this.subscribe<ConstInput<in T>>(asyncBiFunc.param1)
        other.subscribe<ConstInput<in U>>(asyncBiFunc.param2)
        asyncBiFunc.start(executor)
        return asyncBiFunc.asyncResult()
    }

    fun <U> thenAcceptBoth(
            other: CompletablePromise<out U>,
            action: BiConsumer<in T, in U>): CompletablePromise<Void> {
        return thenAcceptBothAsync(other, action, syncExec)
    }

    fun <U> thenAcceptBothAsync(
            other: CompletablePromise<out U>,
            action: BiConsumer<in T, in U>): CompletablePromise<Void> {
        return thenAcceptBothAsync(other, action, asyncExec)
    }

    fun <U> thenAcceptBothAsync(
            other: CompletablePromise<out U>,
            action: BiConsumer<in T, in U>, executor: Executor): CompletablePromise<Void> {
        val asyncBiConsumer = AsyncBiFunction<in T, in U, Void>(action)
        this.subscribe<ConstInput<in T>>(asyncBiConsumer.param1)
        other.subscribe<ConstInput<in U>>(asyncBiConsumer.param2)
        asyncBiConsumer.start(executor)
        return asyncBiConsumer.asyncResult()
    }

    fun <U> runAfterBoth(other: CompletablePromise<out U>,
                         action: Runnable): CompletablePromise<Void> {
        return runAfterBothAsync(other, action, syncExec)
    }

    fun <U> runAfterBothAsync(other: CompletablePromise<out U>,
                              action: Runnable): CompletablePromise<Void> {
        return runAfterBothAsync(other, action, asyncExec)
    }

    fun <U> runAfterBothAsync(other: CompletablePromise<out U>,
                              action: Runnable,
                              executor: Executor): CompletablePromise<Void> {
        val fn = { t, u -> action.run() }
        val asyncBiConsumer = AsyncBiFunction<T, U, Void>(fn)
        this.subscribe<ConstInput<in T>>(asyncBiConsumer.param1)
        other.subscribe<ConstInput<in U>>(asyncBiConsumer.param2)
        asyncBiConsumer.start(executor)
        return asyncBiConsumer.asyncResult()
    }

    fun <U> applyToEither(
            other: CompletablePromise<out T>, fn: Function<in T, U>): CompletablePromise<U> {
        return applyToEitherAsync(other, fn, syncExec)
    }

    fun <U> applyToEitherAsync(
            other: CompletablePromise<out T>, fn: Function<in T, U>): CompletablePromise<U> {
        return applyToEitherAsync(other, fn, asyncExec)
    }

    fun <U> applyToEitherAsync(
            other: CompletablePromise<out T>,
            fn: Function<in T, U>,
            executor: Executor
    ): CompletablePromise<U> {
        val either = AnyOf(this, other)
        val asyncFunc = AsyncFunction<in T, U>(fn)
        either.subscribe<AsyncFunction<in T, U>>(asyncFunc)
        asyncFunc.start(executor)
        return asyncFunc.asyncResult()
    }

    fun acceptEither(
            other: CompletablePromise<out T>, action: Consumer<in T>): CompletablePromise<Void> {
        return acceptEitherAsync(other, action, syncExec)
    }

    @JvmOverloads
    fun acceptEitherAsync(
            other: CompletablePromise<out T>, action: Consumer<in T>,
            executor: Executor = asyncExec): CompletablePromise<Void> {
        val either = AnyOf(this, other)
        val asyncFunc = AsyncFunction<T, Void>(action)
        either.subscribe<AsyncFunction<in T, Void>>(asyncFunc)
        asyncFunc.start(executor)
        return asyncFunc.asyncResult()
    }

    fun runAfterEither(other: CompletablePromise<out T>,
                       action: Runnable): CompletablePromise<Void> {
        return runAfterEitherAsync(other, action, syncExec)
    }

    @JvmOverloads
    fun runAfterEitherAsync(other: CompletablePromise<out T>,
                            action: Runnable,
                            executor: Executor = asyncExec): CompletablePromise<Void> {
        val either = AnyOf(this, other)
        val asyncFunc = AsyncFunction<Any, Void>(action)
        either.subscribe<AsyncFunction<Any, Void>>(asyncFunc)
        asyncFunc.start(executor)
        return asyncFunc.asyncResult()
    }

    fun <U> thenCompose(
            fn: Function<in T, out CompletablePromise<U>>): CompletablePromise<U> {
        return thenComposeAsync(fn, syncExec)
    }

    fun <U> thenComposeAsync(
            fn: Function<in T, out CompletablePromise<U>>): CompletablePromise<U> {
        return thenComposeAsync(fn, asyncExec)
    }

    fun <U> thenComposeAsync(
            fn: Function<in T, out CompletablePromise<U>>,
            executor: Executor): CompletablePromise<U> {
        throw UnsupportedOperationException()
    }

    fun whenComplete(
            action: BiConsumer<in T, in Throwable>): CompletablePromise<T> {
        return whenCompleteAsync(action, syncExec)
    }

    @JvmOverloads
    fun whenCompleteAsync(
            action: BiConsumer<in T, in Throwable>,
            executor: Executor = asyncExec): CompletablePromise<T> {
        val action1 = { arg, ex ->
            action.accept(arg, ex)
            arg
        }
        val asyncHandler = AsyncHandler<T, T>(action1)
        this.subscribe(asyncHandler)
        asyncHandler.start(executor)
        return asyncHandler.asyncResult()
    }

    fun <U> handle(
            fn: BiFunction<in T, Throwable, out U>): CompletablePromise<U> {
        return handleAsync(fn, syncExec)
    }

    fun <U> handleAsync(
            fn: BiFunction<in T, Throwable, out U>): CompletablePromise<U> {
        return handleAsync(fn, asyncExec)
    }

    fun <U> handleAsync(
            fn: BiFunction<in T, Throwable, out U>, executor: Executor): CompletablePromise<U> {
        val handler = AsyncHandler(fn)
        this.subscribe(handler)
        handler.start(executor)
        return handler.asyncResult()
    }

    /**
     * Returns a new CompletablePromise that is completed when this
     * CompletablePromise completes, with the result of the given
     * function of the exception triggering this CompletablePromise's
     * completion when it completes exceptionally; otherwise, if this
     * CompletablePromise completes normally, then the returned
     * CompletablePromise also completes normally with the same value.
     * Note: More flexible versions of this functionality are
     * available using methods `whenComplete` and `handle`.
     *
     * @param fn the function to use to compute the value of the
     * returned CompletablePromise if this CompletablePromise completed
     * exceptionally
     * @return the new CompletablePromise
     */
    fun exceptionally(
            fn: Function<Throwable, out T>): CompletablePromise<T> {
        val handler = { value, ex ->
            return if (ex != null) {
                fn.apply(ex)
            } else {
                value
            }
        }
        return handleAsync(handler)
    }

    /**
     * Forcibly sets or resets the value subsequently returned by
     * method [.get] and related methods, whether or not
     * already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param value the completion value
     */
    fun obtrudeValue(value: T) {
        throw UnsupportedOperationException()
    }

    /**
     * Forcibly causes subsequent invocations of method [.get]
     * and related methods to throw the given exception, whether or
     * not already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param ex the exception
     * @throws NullPointerException if the exception is null
     */
    fun obtrudeException(ex: Throwable) {
        throw UnsupportedOperationException()
    }

    override fun toString(): String {
        val count = numberOfDependents
        val sb = StringBuilder()
        if (completed) {
            sb.append("[Completed normally]")
        } else if (exception != null) {
            sb.append("[Completed exceptionally]")
        } else if (count == 0) {
            sb.append("[Not completed]")
        } else {
            sb.append("[Not completed, ")
            sb.append(count)
            sb.append(" dependents]")
        }
        return sb.toString()
    }

    companion object {
        private val syncExec = SameThreadExecutor.sameThreadExecutor
        private val asyncExec = ForkJoinPool.commonPool()

        /* ------------- Just for fun:  public methods from j.u.c.CompletableFuture -------------- */

        /**
         * Returns a new CompletablePromise that is asynchronously completed
         * by a task running in the [ForkJoinPool.commonPool] with
         * the value obtained by calling the given Supplier.
         *
         * @param supplier a function returning the value to be used
         * to complete the returned CompletablePromise
         * @param <U> the function's return type
         * @return the new CompletablePromise
        </U> */
        fun <U> supplyAsync(supplier: Supplier<U>): CompletablePromise<U> {
            return supplyAsync(supplier, asyncExec)
        }

        /**
         * Returns a new CompletablePromise that is asynchronously completed
         * by a task running in the given executor with the value obtained
         * by calling the given Supplier.
         *
         * @param supplier a function returning the value to be used
         * to complete the returned CompletablePromise
         * @param executor the executor to use for asynchronous execution
         * @param <U> the function's return type
         * @return the new CompletablePromise
        </U> */
        fun <U> supplyAsync(supplier: Supplier<U>,
                            executor: Executor): CompletablePromise<U> {
            val asyncSupplier = AsyncSupplier(supplier)
            asyncSupplier.start(executor)
            return asyncSupplier.asyncResult()
        }

        /**
         * Returns a new CompletablePromise that is asynchronously completed
         * by a task running in the given executor after it runs the given
         * action.
         *
         * @param runnable the action to run before completing the
         * returned CompletablePromise
         * @param executor the executor to use for asynchronous execution
         * @return the new CompletablePromise
         */
        @JvmOverloads
        fun runAsync(runnable: Runnable,
                     executor: Executor = asyncExec): CompletablePromise<Void> {
            val asyncTask = AsyncSupplier<Any>(runnable)
            asyncTask.start(executor)
            return asyncTask.asyncResult()
        }

        /**
         * Returns a new CompletableFuture that is already completed with
         * the given value.
         *
         * @param value the value
         * @param <U> the type of the value
         * @return the completed CompletableFuture
        </U> */
        fun <U> completedFuture(value: U): CompletablePromise<U> {
            val res = CompletablePromise<U>()
            res.complete(value)
            return res
        }

        /* ------------- Arbitrary-arity constructions -------------- */

        /**
         * Returns a new CompletablePromise that is completed when all of
         * the given CompletableFutures complete.  If any of the given
         * CompletableFutures complete exceptionally, then the returned
         * CompletablePromise also does so, with a CompletionException
         * holding this exception as its cause.  Otherwise, the results,
         * if any, of the given CompletableFutures are not reflected in
         * the returned CompletablePromise, but may be obtained by
         * inspecting them individually. If no CompletableFutures are
         * provided, returns a CompletablePromise completed with the value
         * `null`.
         *
         *
         * Among the applications of this method is to await completion
         * of a set of independent CompletableFutures before continuing a
         * program, as in: `CompletablePromise.allOf(c1, c2,
         * c3).join();`.
         *
         * @param cfs the CompletableFutures
         * @return a new CompletablePromise that is completed when all of the
         * given CompletableFutures complete
         * @throws NullPointerException if the array or any of its elements are
         * `null`
         */
        fun allOf(vararg cfs: CompletablePromise<*>): CompletablePromise<Void> {
            val allOf = AllOf(*cfs)
            return allOf.asyncResult()
        }

        /**
         * Returns a new CompletablePromise that is completed when any of
         * the given CompletableFutures complete, with the same result.
         * Otherwise, if it completed exceptionally, the returned
         * CompletablePromise also does so, with a CompletionException
         * holding this exception as its cause.  If no CompletableFutures
         * are provided, returns an incomplete CompletablePromise.
         *
         * @param cfs the CompletableFutures
         * @return a new CompletablePromise that is completed with the
         * result or exception of any of the given CompletableFutures when
         * one completes
         * @throws NullPointerException if the array or any of its elements are
         * `null`
         */
        fun anyOf(vararg cfs: CompletablePromise<*>): ScalarPublisher<Any> {
            return AnyOf<Any>(*cfs)
        }
    }
}
/**
 * Returns a new CompletablePromise that is asynchronously completed
 * by a task running in the [ForkJoinPool.commonPool] after
 * it runs the given action.
 *
 * @param runnable the action to run before completing the
 * returned CompletablePromise
 * @return the new CompletablePromise
 */
