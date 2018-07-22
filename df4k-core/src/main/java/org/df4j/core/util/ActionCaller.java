package org.df4j.core.util;

import org.df4j.core.node.Action;
import org.df4j.core.util.invoker.AbstractInvoker;
import org.df4j.core.util.invoker.Invoker;
import org.df4j.core.util.invoker.MethodInvoker;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActionCaller<R> {
    private final static Class actionAnnotation = Action.class;

    public static Invoker findAction(Object objectWithAction, int argCount) throws NoSuchMethodException {
        Class<?> startClass = objectWithAction.getClass();
        Invoker actionInvoker = null;
        Method resultMethod = null;
        classScan:
        for (Class<?> clazz = startClass; !Object.class.equals(clazz) ;clazz = clazz.getSuperclass()) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field: fields) {
                if (!field.isAnnotationPresent(actionAnnotation)) continue;
                if (!AbstractInvoker.class.isAssignableFrom(field.getType())) {
                    throw new NoSuchMethodException("variable annotated with @Action must have type "+AbstractInvoker.class.getSimpleName());
                }
                field.setAccessible(true);
                AbstractInvoker invoker;
                try {
                    invoker = (AbstractInvoker) field.get(objectWithAction);
                } catch (IllegalAccessException e) {
                    continue;
                }
                if (invoker == null) continue;
                if (invoker.isEmpty()) continue;
                if (actionInvoker != null) {
                    throw new NoSuchMethodException("class "+startClass.getName()+" has more than one non-null field annotated with @Action");
                }
                actionInvoker = invoker;
                break classScan;
            }
            Method[] methods = clazz.getDeclaredMethods();
            for (Method m: methods) {
                if (m.isAnnotationPresent(actionAnnotation)) {
                    if (resultMethod != null) {
                        throw new NoSuchMethodException("in class "+startClass.getName()+" more than one method annotated with @Action");
                    }
                    resultMethod = m;
                }
            }
            if (resultMethod != null) {
                if (resultMethod.getParameterTypes().length != argCount) {
                    throw new NoSuchMethodException("class "+startClass.getName()+" has a method annotated with @Action but with wrong numbers of parameters");
                }
                resultMethod.setAccessible(true);
                actionInvoker = new MethodInvoker(objectWithAction, resultMethod);
                break;
            }
        }
        if (actionInvoker == null) {
            throw new NoSuchMethodException("class "+startClass.getName()+" has no field or method annotated with @Action");
        }
        return actionInvoker;
    }
}
