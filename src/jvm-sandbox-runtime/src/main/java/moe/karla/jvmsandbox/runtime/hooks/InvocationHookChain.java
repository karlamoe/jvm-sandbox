package moe.karla.jvmsandbox.runtime.hooks;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class InvocationHookChain extends InvocationHook {
    private final Iterable<InvocationHook> hooks;

    public InvocationHookChain(Iterable<InvocationHook> hooks) {
        this.hooks = hooks;
    }


    @Override
    public CallSite interpretInvoke(MethodHandles.Lookup caller, Class<?> owner, String methodName, MethodType desc, int refType) throws Throwable {
        for (InvocationHook hook : hooks) {
            var result = hook.interpretInvoke(caller, owner, methodName, desc, refType);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public CallSite interpretInvokeDynamic(MethodHandles.Lookup caller, String methodName, MethodType desc, Class<?> metafactory, String factoryName, MethodType factoryType, Object[] args) throws Throwable {
        for (InvocationHook hook : hooks) {
            var result = hook.interpretInvokeDynamic(caller, methodName, desc, metafactory, factoryName, factoryType, args);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public Object interpretValue(MethodHandles.Lookup caller, Object value) throws Throwable {
        for (InvocationHook hook : hooks) {
            var result = hook.interpretValue(caller, value);
            if (result != null) return result;
        }
        return null;
    }
}
