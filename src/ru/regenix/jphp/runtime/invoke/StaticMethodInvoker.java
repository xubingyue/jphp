package ru.regenix.jphp.runtime.invoke;

import ru.regenix.jphp.common.Messages;
import ru.regenix.jphp.exceptions.FatalException;
import ru.regenix.jphp.runtime.env.Environment;
import ru.regenix.jphp.runtime.env.TraceInfo;
import ru.regenix.jphp.runtime.memory.support.Memory;
import ru.regenix.jphp.runtime.reflection.ClassEntity;
import ru.regenix.jphp.runtime.reflection.MethodEntity;

import java.lang.reflect.InvocationTargetException;

public class StaticMethodInvoker extends Invoker {
    protected final MethodEntity method;
    protected final String calledClass;

    public StaticMethodInvoker(Environment env, TraceInfo trace, String calledClass, MethodEntity method) {
        super(env, trace);
        this.method = method;
        this.calledClass = calledClass;
    }

    @Override
    public void pushCall(TraceInfo trace, Memory[] args) {
        env.pushCall(trace, null, args, method.getName(), calledClass);
    }

    @Override
    public Memory call(Memory... args) throws InvocationTargetException, IllegalAccessException {
        return InvokeHelper.callStatic(env, trace, method, args);
    }

    public static StaticMethodInvoker valueOf(Environment env, TraceInfo trace, String className, String methodName){
        className = className.toLowerCase();
        MethodEntity methodEntity = env.scope.methodMap.get(
                className + "#" + methodName.toLowerCase()
        );

        if (methodEntity == null){
            if (trace == null) {
                ClassEntity __class__ = env.scope.classMap.get(className);
                if (__class__ != null && __class__.methodMagicCallStatic != null){
                    return new MagicStaticMethodInvoker(
                            env, trace, className, __class__.methodMagicCallStatic, methodName
                    );
                }
                return null;
            }
            env.triggerError(new FatalException(
                    Messages.ERR_FATAL_CALL_TO_UNDEFINED_METHOD.fetch(className +"::"+ methodName),
                    trace
            ));
        }

        return new StaticMethodInvoker(env, trace, "", methodEntity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StaticMethodInvoker)) return false;

        StaticMethodInvoker that = (StaticMethodInvoker) o;

        if (!calledClass.equals(that.calledClass)) return false;
        if (!method.equals(that.method)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = method.hashCode();
        result = 31 * result + calledClass.hashCode();
        return result;
    }
}
