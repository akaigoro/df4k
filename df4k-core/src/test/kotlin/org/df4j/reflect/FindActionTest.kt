package org.df4j.reflect

import org.junit.Test

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.security.InvalidParameterException
import java.util.Arrays
import java.util.concurrent.Callable
import java.util.function.BiFunction
import java.util.function.Consumer

class FindActionTest {
    internal var r = { }
    internal var c1 = Consumer<String> { println(it) }
    internal var f2 = BiFunction<String, Int, Double> { s, n -> this.m2(s, n) }
    internal var f3 = I3 { s, n, f -> this.m3(s, n, f) }
    internal var ff = F2()

    internal interface I3 {
        fun m3(s: String, n: Int, f: Float): Double
    }

    internal class F2 {
        fun m2(s: String, n: Int): Float {
            return n.toFloat()
        }

    }

    fun m2(s: String, n: Int): Double {
        return n.toDouble()
    }

    fun m3(s: String, n: Int, f: Float): Double {
        return n.toDouble()
    }

    internal fun printType(vararg args: Any) {
        for (arg in args) {
            printType1(arg)
        }
    }

    private fun printType1(arg: Any) {
        println("type=" + arg.javaClass)
        try {
            val m = getMethodForFunctionalInterface(arg)
            println("method=" + m!!)
        } catch (e: Exception) {
            println("exception$e")
        }

    }

    @Test
    fun lambdaType() {
        printType(r, c1, f2, f3, ff)
    }

    @Throws(Exception::class)
    fun callWhatever(o: Any, vararg params: Any): Any? {
        if (o is Runnable) {
            o.run()
            return null
        }

        if (o is Callable<*>) {
            return o.call()
        }

        if (o is Method) {
            return o.invoke(params[0], *Arrays.copyOfRange(params, 1, params.size))
        }

        val method = getMethodForFunctionalInterface(o)
        if (method != null) {
            return method.invoke(o, *params)
        }

        throw InvalidParameterException("Object of type " + o.javaClass + " is not callable!")
    }

    fun getMethodForFunctionalInterface(o: Any): Method? {
        var clazz: Class<*>? = o.javaClass
        while (clazz != null) {
            for (interfaze in clazz.interfaces) {
                for (method in interfaze.declaredMethods) {
                    if (Modifier.isAbstract(method.modifiers)) {
                        return method
                    }
                }
            }
            clazz = clazz.superclass
        }
        return null
    }
}
