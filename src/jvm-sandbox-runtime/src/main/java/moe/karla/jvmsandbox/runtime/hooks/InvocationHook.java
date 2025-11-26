package moe.karla.jvmsandbox.runtime.hooks;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.util.RuntimeResolvationInfo;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

public class InvocationHook {
    public CallSite interpretInvoke(
            SandboxRuntime runtime,
            MethodHandles.Lookup caller,
            Class<?> owner,
            String methodName,
            MethodType desc,
            int refType,
            RuntimeResolvationInfo callInfo
    ) throws Throwable {
        return null;
    }

    public CallSite interpretInvokeDynamic(
            SandboxRuntime runtime,
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
            SandboxRuntime runtime,
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
            SandboxRuntime runtime,
            MethodHandles.Lookup caller,
            Object value
    ) throws Throwable {
        return null;
    }

    public CallSite interpretBeforeObjectConstruct(
            SandboxRuntime runtime, MethodHandles.Lookup caller,
            Class<?> target, MethodType methodType
    ) throws Throwable {
        return null;
    }
}
