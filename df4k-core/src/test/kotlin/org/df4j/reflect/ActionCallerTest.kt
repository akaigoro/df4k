package org.df4j.reflect

import org.df4j.core.node.Action
import org.df4j.core.util.ActionCaller
import org.df4j.core.util.invoker.FunctionInvoker
import org.df4j.core.util.invoker.Invoker
import org.junit.Assert
import org.junit.Test

import java.util.function.Function

class ActionCallerTest {

    @Test(expected = NoSuchMethodException::class)
    @Throws(NoSuchMethodException::class)
    fun emptyClass_0() {
        ActionCaller.findAction(Empty(), 0)
    }

    @Test(expected = NoSuchMethodException::class)
    @Throws(NoSuchMethodException::class)
    fun emptyClass_1() {
        ActionCaller.findAction(Empty(), 1)
    }

    @Test(expected = NoSuchMethodException::class)
    @Throws(NoSuchMethodException::class)
    fun manyFields() {
        ActionCaller.findAction(WithField_2(), 1)
    }

    @Test(expected = NoSuchMethodException::class)
    @Throws(NoSuchMethodException::class)
    fun nullField() {
        ActionCaller.findAction(WithField_1(null), 1)
    }

    @Test
    @Throws(Exception::class)
    fun notNullField() {
        val f = { v -> v!! * 2 }
        val ac = ActionCaller.findAction(WithField_1(f), 1)
        Assert.assertTrue(ac.returnsValue())
        val res1 = ac.apply(ONE) as Int
        Assert.assertEquals(TWO, res1)
        val res2 = ac.apply(TWO) as Int
        Assert.assertEquals(FOUR, res2)
    }

    @Test
    @Throws(Exception::class)
    fun fieldWithAlienMethod() {
        val wfm = WithFieldAndMethod(null)
        val f = Function<Int, Int> { wfm.dec(it) }
        val ac = ActionCaller.findAction(WithField_1(f), 1)
        Assert.assertTrue(ac.returnsValue())
        var res1: Int? = ac.apply(TWO) as Int
        Assert.assertEquals(ONE, res1)
        res1 = ac.apply(FOUR) as Int
        Assert.assertEquals(THREE, res1)
    }

    @Test
    @Throws(Exception::class)
    fun fieldWithStaticMethod() {
        val f = Function<Int, Int> { WithFieldAndMethod.inc(it) }
        val ac = ActionCaller.findAction(WithField_1(f), 1)
        Assert.assertTrue(ac.returnsValue())
        val res1 = ac.apply(ONE) as Int
        Assert.assertEquals(TWO, res1)
        val res2 = ac.apply(TWO) as Int
        Assert.assertEquals(THREE, res2)
    }

    @Test
    @Throws(Exception::class)
    fun nullFieldAndMethod() {
        val ac = ActionCaller.findAction(WithFieldAndMethod(null), 1)
        Assert.assertTrue(ac.returnsValue())
        var res1: Int? = ac.apply(TWO) as Int
        Assert.assertEquals(ONE, res1)
        res1 = ac.apply(FOUR) as Int
        Assert.assertEquals(THREE, res1)
    }

    @Test
    @Throws(Exception::class)
    fun notNullFieldAndMethod() {
        val f = { v -> v!! * 2 }
        val ac = ActionCaller.findAction(WithFieldAndMethod(f), 1)
        val res1 = ac.apply(ONE) as Int
        Assert.assertEquals(TWO, res1)
        val res2 = ac.apply(TWO) as Int
        Assert.assertEquals(FOUR, res2)
    }

    @Test
    @Throws(Exception::class)
    fun methodOnly() {
        val p = ActionCaller.findAction(WithProc(), 0)
        Assert.assertFalse(p.returnsValue())
        val res = p.apply()
        Assert.assertNull(res)
        val f = ActionCaller.findAction(WithFunc(), 0)
        Assert.assertTrue(f.returnsValue())
        val res2 = f.apply()
        Assert.assertEquals(137, res2)
    }


    internal class Empty

    internal class WithField_1(function: Function<*, *>) {
        @Action
        val invoker: FunctionInvoker<*, *>

        init {
            this.invoker = FunctionInvoker(function)
        }
    }

    internal class WithField_2 {
        @Action
        var function1: Function<*, *>? = null
        @Action
        var function2: Function<*, *>? = null
    }

    internal class WithFieldAndMethod(function: Function<*, *>) {
        @Action
        val invoker: FunctionInvoker<*, *>

        init {
            this.invoker = FunctionInvoker(function)
        }

        @Action
        fun dec(arg: Int): Int {
            return arg - 1
        }

        companion object {

            fun inc(arg: Int): Int {
                return arg + 1
            }
        }
    }

    internal class WithProc {
        @Action
        fun proc() {

        }
    }

    internal class WithFunc {
        @Action
        fun func(): Int {
            return 137
        }
    }

    companion object {
        internal val ONE = Integer.valueOf(1)
        internal val TWO = Integer.valueOf(2)
        internal val THREE = Integer.valueOf(3)
        internal val FOUR = Integer.valueOf(4)
    }
}
