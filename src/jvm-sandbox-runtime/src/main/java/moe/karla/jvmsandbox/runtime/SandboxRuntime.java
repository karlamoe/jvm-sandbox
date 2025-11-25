package moe.karla.jvmsandbox.runtime;

import moe.karla.jvmsandbox.runtime.hooks.InvocationHook;

import java.lang.invoke.*;
import java.util.ArrayList;
import java.util.Arrays;

public class SandboxRuntime extends InvocationHook {
    @Override
    public CallSite interpretInvoke(
            MethodHandles.Lookup caller,
            Class<?> owner,
            String methodName,
            MethodType desc,
            int refType
    ) throws Throwable {
        return new ConstantCallSite((switch (refType) {
            case MethodHandleInfo.REF_getField -> caller.findGetter(owner, methodName, desc.returnType());
            case MethodHandleInfo.REF_putField -> caller.findSetter(owner, methodName, desc.parameterType(1));
            case MethodHandleInfo.REF_getStatic -> caller.findStaticGetter(owner, methodName, desc.returnType());
            case MethodHandleInfo.REF_putStatic -> caller.findStaticSetter(owner, methodName, desc.parameterType(0));

            case MethodHandleInfo.REF_invokeStatic -> caller.findStatic(owner, methodName, desc);
            case MethodHandleInfo.REF_invokeSpecial ->
                    caller.findSpecial(owner, methodName, desc.dropParameterTypes(0, 1), caller.lookupClass());
            case MethodHandleInfo.REF_invokeInterface, MethodHandleInfo.REF_invokeVirtual ->
                    caller.findVirtual(owner, methodName, desc.dropParameterTypes(0, 1));

            case MethodHandleInfo.REF_newInvokeSpecial ->
                    caller.findConstructor(owner, desc.changeReturnType(void.class));

            default -> throw new IllegalArgumentException(
                    "Unknown refType: " + refType + ": " + owner + '.' + methodName + desc
            );
        }).asType(desc));
    }

    @Override
    public CallSite interpretInvokeDynamic(
            MethodHandles.Lookup caller, String methodName, MethodType desc,
            Class<?> metafactory, String factoryName,
            MethodType factoryType,
            Object[] args
    ) throws Throwable {
        var handle = caller.findStatic(metafactory, factoryName, factoryType);
        var args0 = new ArrayList<>();
        args0.add(caller);
        args0.add(methodName);
        args0.add(desc);
        if (args != null) {
            args0.addAll(Arrays.asList(args));
        }

        if (!handle.isVarargsCollector()) {
            while (handle.type().parameterCount() > 0) {
                handle = handle.bindTo(args0.removeFirst());
            }
        } else {
            while (handle.type().parameterCount() > 1) {
                handle = handle.bindTo(args0.removeFirst());
            }
            handle = handle.asFixedArity().bindTo(args0.toArray());
        }
        return (CallSite) handle.invoke();
    }
}
