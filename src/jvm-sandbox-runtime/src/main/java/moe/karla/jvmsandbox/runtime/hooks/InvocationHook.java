package moe.karla.jvmsandbox.runtime.hooks;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

public class InvocationHook {
    public CallSite interpretInvoke(
            MethodHandles.Lookup caller,
            Class<?> owner,
            String methodName,
            MethodType desc,
            int refType
    ) throws Throwable {
        return null;
    }

    public CallSite interpretInvokeDynamic(
            MethodHandles.Lookup caller,
            String methodName,
            MethodType desc,
            Class<?> metafactory,
            String factoryName,
            MethodType factoryType,
            Object[] args
    ) throws Throwable {
        return null;
    }

    // special case: null -> no hook care about it
    public @Nullable Optional<?> interpretInvokeDynamicConstant(
            MethodHandles.Lookup caller,
            String methodName,
            Class<?> resultType,
            Class<?> metafactory,
            String factoryName,
            MethodType factoryType,
            Object[] args
    ) throws Throwable {
        return null;
    }

    public Object interpretValue(
            MethodHandles.Lookup caller,
            Object value
    ) throws Throwable {
        return null;
    }
}
