package org.df4k.core.util

import org.df4k.core.node.Action
import org.df4k.core.util.invoker.AbstractInvoker
import org.df4k.core.util.invoker.Invoker
import org.df4k.core.util.invoker.MethodInvoker

import java.lang.reflect.Field
import java.lang.reflect.Method

class ActionCaller<R> {
    companion object {
        private val actionAnnotation = Action::class.java

        @Throws(NoSuchMethodException::class)
        fun findAction(objectWithAction: Any, argCount: Int): Invoker<*> {
            val startClass = objectWithAction.javaClass
            var actionInvoker: Invoker<*>? = null
            var resultMethod: Method? = null
            var clazz = startClass
            classScan@ while (Any::class.java != clazz) {
                val fields = clazz.declaredFields
                for (field in fields) {
                    if (!field.isAnnotationPresent(actionAnnotation)) continue
                    if (!AbstractInvoker<*, *>::class.java.isAssignableFrom(field.type)) {
                        throw NoSuchMethodException("variable annotated with @Action must have type " + AbstractInvoker<*, *>::class.java.simpleName)
                    }
                    field.isAccessible = true
                    val invoker: AbstractInvoker<*, *>?
                    try {
                        invoker = field.get(objectWithAction) as AbstractInvoker<*, *>
                    } catch (e: IllegalAccessException) {
                        continue
                    }

                    if (invoker == null) continue
                    if (invoker.isEmpty) continue
                    if (actionInvoker != null) {
                        throw NoSuchMethodException("class " + startClass.name + " has more than one non-null field annotated with @Action")
                    }
                    actionInvoker = invoker
                    break@classScan
                }
                val methods = clazz.declaredMethods
                for (m in methods) {
                    if (m.isAnnotationPresent(actionAnnotation)) {
                        if (resultMethod != null) {
                            throw NoSuchMethodException("in class " + startClass.name + " more than one method annotated with @Action")
                        }
                        resultMethod = m
                    }
                }
                if (resultMethod != null) {
                    if (resultMethod.parameterTypes.size != argCount) {
                        throw NoSuchMethodException("class " + startClass.name + " has a method annotated with @Action but with wrong numbers of parameters")
                    }
                    resultMethod.isAccessible = true
                    actionInvoker = MethodInvoker(objectWithAction, resultMethod)
                    break
                }
                clazz = clazz.getSuperclass()
            }
            if (actionInvoker == null) {
                throw NoSuchMethodException("class " + startClass.name + " has no field or method annotated with @Action")
            }
            return actionInvoker
        }
    }
}
