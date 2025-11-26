package moe.karla.jvmsandbox.runtime.helper;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.hooks.InvocationHook;

import java.lang.invoke.*;

public class ReflectionRedirectHook extends InvocationHook {
    private static final MethodHandle MH_CallSite$dynamicInvoker;
    private static final MethodHandle MH_interpretInvoke$CallSite;
    private static final MethodHandle MH_interpretInvoke$MethodHandle;
    private static final MethodHandle MH_adapter$findConstructor;
    private static final MethodHandle MH_adapter$findSpecial;
    private static final MethodHandle MH_adapter$findVirtual;
    private static final MethodHandle MH_adapter$findGetterSetter;

    static {
        var lookup = MethodHandles.lookup();
        try {
            MH_interpretInvoke$CallSite = lookup.findVirtual(SandboxRuntime.class, "interpretInvoke", MethodType.methodType(
                    CallSite.class, MethodHandles.Lookup.class, Class.class, String.class, MethodType.class, int.class
            ));
            MH_CallSite$dynamicInvoker = lookup.findVirtual(
                    CallSite.class, "dynamicInvoker",
                    MethodType.methodType(MethodHandle.class)
            );
            MH_interpretInvoke$MethodHandle = MethodHandles.filterReturnValue(
                    MH_interpretInvoke$CallSite,
                    MH_CallSite$dynamicInvoker
            );

            MH_adapter$findConstructor = lookup.findStatic(ReflectionRedirectHook.class, "adapter$findConstructor", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, MethodHandles.Lookup.class, Class.class, MethodType.class));
            MH_adapter$findSpecial = lookup.findStatic(ReflectionRedirectHook.class, "adapter$findSpecial", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, MethodHandles.Lookup.class, Class.class, String.class, MethodType.class, Class.class));
            MH_adapter$findVirtual = lookup.findStatic(ReflectionRedirectHook.class, "adapter$findVirtual", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, MethodHandles.Lookup.class, Class.class, String.class, MethodType.class));
            MH_adapter$findGetterSetter = lookup.findStatic(ReflectionRedirectHook.class, "adapter$findGetterSetter", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, int.class, MethodHandles.Lookup.class, Class.class, String.class, Class.class));
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static MethodHandle adapter$findConstructor(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> target, MethodType desc) throws Throwable {
        return runtime.interpretInvoke(caller, target, "<init>", desc.changeReturnType(target), MethodHandleInfo.REF_newInvokeSpecial)
                .dynamicInvoker();
    }

    private static MethodHandle adapter$findVirtual(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> target, String name, MethodType desc) throws Throwable {
        return runtime.interpretInvoke(caller, target, name, desc.insertParameterTypes(0, target), MethodHandleInfo.REF_invokeVirtual)
                .dynamicInvoker();
    }

    private static MethodHandle adapter$findSpecial(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> target, String name, MethodType desc, Class<?> specialCaller) throws Throwable {
        // TODO specialCaller

        return runtime.interpretInvoke(caller, target, name, desc.insertParameterTypes(0, caller.lookupClass()), MethodHandleInfo.REF_invokeSpecial)
                .dynamicInvoker();
    }

    private static MethodHandle adapter$findGetterSetter(SandboxRuntime runtime, int refType, MethodHandles.Lookup caller, Class<?> target, String name, Class<?> type) throws Throwable {
        return switch (refType) {
            case MethodHandleInfo.REF_getField -> runtime.interpretInvoke(
                    caller, target, name, MethodType.methodType(type, target), refType
            ).dynamicInvoker();
            case MethodHandleInfo.REF_putField -> runtime.interpretInvoke(
                    caller, target, name, MethodType.methodType(void.class, target, type), refType
            ).dynamicInvoker();

            case MethodHandleInfo.REF_getStatic -> runtime.interpretInvoke(
                    caller, target, name, MethodType.methodType(type), refType
            ).dynamicInvoker();
            case MethodHandleInfo.REF_putStatic -> runtime.interpretInvoke(
                    caller, target, name, MethodType.methodType(void.class, type), refType
            ).dynamicInvoker();

            default -> throw new AssertionError();
        };
    }

    @Override
    public CallSite interpretInvoke(
            SandboxRuntime runtime, MethodHandles.Lookup caller,
            Class<?> owner, String methodName, MethodType desc,
            int refType
    ) throws Throwable {
        if (owner == MethodHandles.Lookup.class) {
            switch (methodName) {

                // invokes
                case "findStatic" -> {
                    // Lookup.(Class<?>, String, MethodType)MethodHandle
                    return new ConstantCallSite(
                            MethodHandles.insertArguments(MH_interpretInvoke$MethodHandle, 5, MethodHandleInfo.REF_invokeStatic)
                                    .bindTo(runtime)
                    );
                }
                case "findVirtual" -> {
                    // Lookup.(Class<?>, String name, MethodType)MethodHandle
                    return new ConstantCallSite(MH_adapter$findVirtual.bindTo(runtime));

                }
                case "findConstructor" -> {
                    // Lookup.(Class<?>, MethodType)MethodHandle
                    return new ConstantCallSite(MH_adapter$findConstructor.bindTo(runtime));
                }
                case "findSpecial" -> {
                    // Lookup.(Class<?>, String, MethodType, Class<?>)MethodHandle
                    return new ConstantCallSite(MH_adapter$findSpecial.bindTo(runtime));
                }

                // fields
                case "findGetter" -> {
                    return new ConstantCallSite(MH_adapter$findGetterSetter.bindTo(runtime).bindTo(MethodHandleInfo.REF_getField));
                }
                case "findSetter" -> {
                    return new ConstantCallSite(MH_adapter$findGetterSetter.bindTo(runtime).bindTo(MethodHandleInfo.REF_putField));
                }
                case "findStaticGetter" -> {
                    return new ConstantCallSite(MH_adapter$findGetterSetter.bindTo(runtime).bindTo(MethodHandleInfo.REF_getStatic));
                }
                case "findStaticSetter" -> {
                    return new ConstantCallSite(MH_adapter$findGetterSetter.bindTo(runtime).bindTo(MethodHandleInfo.REF_putStatic));
                }


                case "findVarHandle" -> {
                    // TODO
                }
                case "findStaticVarHandle" -> {
                    // TODO
                }


                case "unreflect" -> {
                }
                case "unreflectSpecial" -> {
                }
                case "unreflectConstructor" -> {
                }
                case "unreflectGetter" -> {
                }
                case "unreflectSetter" -> {
                }
                case "unreflectVarHandle" -> {
                }
                case "revealDirect" -> {
                    // TODO
                }
            }
        }
        return null;
    }
}
