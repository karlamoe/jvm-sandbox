package moe.karla.jvmsandbox.runtime;

import moe.karla.jvmsandbox.runtime.hooks.InvocationHook;
import moe.karla.jvmsandbox.runtime.hooks.InvocationHookChain;
import moe.karla.jvmsandbox.runtime.util.InvokeHelper;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class SandboxRuntime extends InvocationHook {
    private final Collection<InvocationHook> hooks;
    private final InvocationHookChain chain;

    public SandboxRuntime() {
        this(new ArrayList<>());
    }

    public SandboxRuntime(Collection<InvocationHook> hooks) {
        this.hooks = hooks;
        this.chain = new InvocationHookChain(hooks);
    }

    public void addHook(InvocationHook hook) {
        this.hooks.add(hook);
    }


    @Override
    public CallSite interpretInvoke(
            MethodHandles.Lookup caller,
            Class<?> owner,
            String methodName,
            MethodType desc,
            int refType
    ) throws Throwable {
        var result = chain.interpretInvoke(caller, owner, methodName, desc, refType);
        if (result != null) return result;

        return new ConstantCallSite(InvokeHelper.resolveMethodHandle(caller, owner, methodName, desc, refType).asType(desc));
    }

    @Override
    public Object interpretValue(MethodHandles.Lookup caller, Object value) throws Throwable {
        var result = chain.interpretValue(caller, value);
        if (result != null) return result;

        if (value instanceof MethodHandle argHandle) {
            var handleInfo = caller.revealDirect(argHandle);
            return interpretInvoke(
                    caller,
                    handleInfo.getDeclaringClass(),
                    handleInfo.getName(),
                    handleInfo.getMethodType(),
                    handleInfo.getReferenceKind()
            ).dynamicInvoker();
        } else {
            return value;
        }
    }

    @Override
    public CallSite interpretInvokeDynamic(
            MethodHandles.Lookup caller, String methodName, MethodType desc,
            Class<?> metafactory, String factoryName,
            MethodType factoryType,
            Object[] args
    ) throws Throwable {
        var result = chain.interpretInvokeDynamic(caller, methodName, desc, metafactory, factoryName, factoryType, args);
        if (result != null) return result;

        var handle = caller.findStatic(metafactory, factoryName, factoryType);
        var args0 = new ArrayList<>();
        args0.add(caller);
        args0.add(methodName);
        args0.add(desc);
        if (args != null) {
            for (var arg : args) {
                args0.add(interpretValue(caller, arg));
            }
        }

        return (CallSite) handle.invokeWithArguments(args0);
    }


    @Override
    public @Nullable Optional<?> interpretInvokeDynamicConstant(
            MethodHandles.Lookup caller, String methodName, Class<?> resultType,
            Class<?> metafactory, String factoryName, MethodType factoryType,
            Object[] args
    ) throws Throwable {
        var result = chain.interpretInvokeDynamicConstant(caller, methodName, resultType, metafactory, factoryName, factoryType, args);
        if (result != null) return result;


        var handle = caller.findStatic(metafactory, factoryName, factoryType);
        var args0 = new ArrayList<>();
        args0.add(caller);
        args0.add(methodName);
        args0.add(resultType);
        if (args != null) {
            for (var arg : args) {
                args0.add(interpretValue(caller, arg));
            }
        }

        return Optional.ofNullable(handle.invokeWithArguments(args0));
    }
}
