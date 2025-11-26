package moe.karla.jvmsandbox.runtime.hooks;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.util.RuntimeResolvationInfo;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

public class InvocationHookChain extends InvocationHook {
    private final Iterable<InvocationHook> hooks;

    public InvocationHookChain(Iterable<InvocationHook> hooks) {
        this.hooks = hooks;
    }


    @Override
    public CallSite interpretInvoke(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> owner, String methodName, MethodType desc, int refType, RuntimeResolvationInfo callInfo) throws Throwable {
        for (InvocationHook hook : hooks) {
            var result = hook.interpretInvoke(runtime, caller, owner, methodName, desc, refType, callInfo);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public CallSite interpretInvokeDynamic(SandboxRuntime runtime, MethodHandles.Lookup caller, String methodName, MethodType desc, Class<?> metafactory, String factoryName, MethodType factoryType, Object[] args) throws Throwable {
        for (InvocationHook hook : hooks) {
            var result = hook.interpretInvokeDynamic(runtime, caller, methodName, desc, metafactory, factoryName, factoryType, args);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public Object interpretValue(SandboxRuntime runtime, MethodHandles.Lookup caller, Object value) throws Throwable {
        for (InvocationHook hook : hooks) {
            var result = hook.interpretValue(runtime, caller, value);
            if (result != null) return result;
        }
        return null;
    }

    @SuppressWarnings("OptionalAssignedToNull")
    @Override
    public @Nullable Optional<?> interpretInvokeDynamicConstant(SandboxRuntime runtime, MethodHandles.Lookup caller, String methodName, Class<?> resultType, Class<?> metafactory, String factoryName, MethodType factoryType, Object[] args) throws Throwable {
        for (InvocationHook hook : hooks) {
            var result = hook.interpretInvokeDynamicConstant(runtime, caller, methodName, resultType, metafactory, factoryName, factoryType, args);
            if (result != null) return result;
        }
        return null;
    }
}
