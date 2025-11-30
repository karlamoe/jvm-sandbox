package moe.karla.jvmsandbox.runtime.hooks;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.util.InvokeHelper;
import moe.karla.jvmsandbox.runtime.util.RuntimeResolvationInfo;

import java.lang.invoke.*;

public class FakedInvocationHook extends InvocationHook {
    @Override
    public CallSite interpretInvoke(
            SandboxRuntime runtime, MethodHandles.Lookup caller,
            Class<?> owner, String methodName, MethodType desc,
            int refType, RuntimeResolvationInfo callInfo
    ) throws Throwable {
        var result = interpretInvoke0(runtime, caller, owner, methodName, desc, refType, callInfo);
        if (result != null && callInfo != null) {
            runtime.reflectionCache.pushFakedSource(result,
                    () -> InvokeHelper.resolveMethodHandle(caller, owner, methodName, desc, refType, callInfo)
            );
        }

        if (result != null) return new ConstantCallSite(result);
        return null;
    }


    public MethodHandle interpretInvoke0(
            SandboxRuntime runtime, MethodHandles.Lookup caller,
            Class<?> owner, String methodName, MethodType desc,
            int refType, RuntimeResolvationInfo callInfo
    ) throws Throwable {
        return null;
    }
}
