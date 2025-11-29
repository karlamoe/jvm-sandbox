package moe.karla.jvmsandbox.runtime;

import moe.karla.jvmsandbox.runtime.hooks.InvocationHook;
import moe.karla.jvmsandbox.runtime.hooks.InvocationHookChain;
import moe.karla.jvmsandbox.runtime.util.InvokeHelper;
import moe.karla.jvmsandbox.runtime.util.ReflectionCache;
import moe.karla.jvmsandbox.runtime.util.RuntimeResolvationInfo;
import moe.karla.jvmsandbox.runtime.util.Symbol;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SandboxRuntime {
    private final Collection<InvocationHook> hooks;
    private final InvocationHookChain chain;

    public final ReflectionCache reflectionCache = new ReflectionCache();
    public final Map<Symbol, Object> runtimeCache = new ConcurrentHashMap<>();

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

    public CallSite interpretInvoke(
            MethodHandles.Lookup caller, Class<?> owner,
            String methodName, MethodType desc, int refType
    ) throws Throwable {
        return interpretInvoke(caller, owner, methodName, desc, refType, null);
    }

    public CallSite interpretInvoke(
            MethodHandles.Lookup caller, Class<?> owner, String methodName,
            MethodType desc, int refType, RuntimeResolvationInfo callInfo
    ) throws Throwable {
        var result = chain.interpretInvoke(this, caller, owner, methodName, desc, refType, callInfo);
        if (result != null) return result;

        if (refType == InvokeHelper.EXREF_beforeConstructor) {
            result = chain.interpretBeforeObjectConstruct(this, caller, owner, desc);
            if (result != null) return result;

            return new ConstantCallSite(MethodHandles.dropReturn(MethodHandles.dropArguments(
                    MethodHandles.constant(Object.class, null),
                    0,
                    desc.parameterList()
            )));
        }

        var realHandle = InvokeHelper.resolveMethodHandle(caller, owner, methodName, desc, refType, callInfo).asType(desc);
        var resultHandle = realHandle;
        if (refType == MethodHandleInfo.REF_newInvokeSpecial) {
            var guard = chain.interpretBeforeObjectConstruct(this, caller, owner, desc.changeReturnType(void.class));
            if (guard != null) {
                resultHandle = MethodHandles.foldArguments(
                        realHandle,
                        MethodHandles.dropReturn(guard.dynamicInvoker())
                );

                if (callInfo != null) {
                    reflectionCache.pushFakedSource(resultHandle, realHandle);
                }
            }
        }
        return new ConstantCallSite(resultHandle);
    }

    public Object interpretValue(MethodHandles.Lookup caller, Object value) throws Throwable {
        var result = chain.interpretValue(this, caller, value);
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

    public CallSite interpretInvokeDynamic(
            MethodHandles.Lookup caller, String methodName, MethodType desc,
            Class<?> metafactory, String factoryName,
            MethodType factoryType,
            Object[] args
    ) throws Throwable {
        var result = chain.interpretInvokeDynamic(this, caller, methodName, desc, metafactory, factoryName, factoryType, args);
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


    @SuppressWarnings("OptionalAssignedToNull")
    public @Nullable Optional<?> interpretInvokeDynamicConstant(
            MethodHandles.Lookup caller, String methodName, Class<?> resultType,
            Class<?> metafactory, String factoryName, MethodType factoryType,
            Object[] args
    ) throws Throwable {
        var result = chain.interpretInvokeDynamicConstant(this, caller, methodName, resultType, metafactory, factoryName, factoryType, args);
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
