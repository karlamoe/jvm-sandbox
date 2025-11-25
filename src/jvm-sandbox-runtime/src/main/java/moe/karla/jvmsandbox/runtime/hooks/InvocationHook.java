package moe.karla.jvmsandbox.runtime.hooks;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class InvocationHook {
    public CallSite interpretInvoke(
            MethodHandles.Lookup caller,
            Class<?> owner,
            String methodName,
            MethodType desc,
            int refType
    ) {
        return null;
    }

    public CallSite interpretInvokeDynamic(
            MethodHandles.Lookup caller,
            String methodName,
            MethodType desc,
            Class<?> metafactory,
            String factoryName,
            MethodType methodType,
            Object[] args
    ) {
        return null;
    }
}
