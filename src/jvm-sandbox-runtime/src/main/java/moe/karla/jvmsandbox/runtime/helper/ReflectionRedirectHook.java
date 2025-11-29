package moe.karla.jvmsandbox.runtime.helper;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.hooks.InvocationHook;
import moe.karla.jvmsandbox.runtime.util.InvokeHelper;
import moe.karla.jvmsandbox.runtime.util.RuntimeResolvationInfo;
import moe.karla.jvmsandbox.runtime.util.Symbol;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class ReflectionRedirectHook extends InvocationHook {
    private static final MethodHandle MH_CallSite$dynamicInvoker;
    private static final MethodHandle MH_interpretInvoke$CallSite;
    private static final MethodHandle MH_interpretInvoke$MethodHandle;
    private static final MethodHandle MH_adapter$findConstructor;
    private static final MethodHandle MH_adapter$findSpecial;
    private static final MethodHandle MH_adapter$findStatic;
    private static final MethodHandle MH_adapter$findVirtual;
    private static final MethodHandle MH_adapter$findGetterSetter;
    private static final MethodHandle MH_adapter$methodInvoke;
    private static final MethodHandle MH_adapter$constructorInvoke;
    private static final MethodHandle MH_adapter$classNewInstance;
    private static final MethodHandle MH_adapter$unreflect;
    private static final MethodHandle MH_adapter$unreflectSpecial;
    private static final MethodHandle MH_adapter$unreflectConstructor;
    private static final MethodHandle MH_adapter$unreflectField;
    private static final MethodHandle MH_adapter$unreflectVarHandle;
    private static final MethodHandle MH_adapter$Field$getOrSet;
    private static final MethodHandle MH_adapter$findVarHandle;
    private static final MethodHandle MH_adapter$revealDirect;

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
            MH_adapter$findStatic = lookup.findStatic(ReflectionRedirectHook.class, "adapter$findStatic", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, MethodHandles.Lookup.class, Class.class, String.class, MethodType.class));
            MH_adapter$findVirtual = lookup.findStatic(ReflectionRedirectHook.class, "adapter$findVirtual", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, MethodHandles.Lookup.class, Class.class, String.class, MethodType.class));
            MH_adapter$findGetterSetter = lookup.findStatic(ReflectionRedirectHook.class, "adapter$findGetterSetter", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, int.class, MethodHandles.Lookup.class, Class.class, String.class, Class.class));
            MH_adapter$methodInvoke = lookup.findStatic(ReflectionRedirectHook.class, "adapter$methodInvoke", MethodType.methodType(Object.class, SandboxRuntime.class, MethodHandles.Lookup.class, Method.class, Object.class, Object[].class));
            MH_adapter$constructorInvoke = lookup.findStatic(ReflectionRedirectHook.class, "adapter$constructorInvoke", MethodType.methodType(Object.class, SandboxRuntime.class, MethodHandles.Lookup.class, Constructor.class, Object[].class));
            MH_adapter$classNewInstance = lookup.findStatic(ReflectionRedirectHook.class, "adapter$classNewInstance", MethodType.methodType(Object.class, SandboxRuntime.class, MethodHandles.Lookup.class, Class.class));
            MH_adapter$unreflect = lookup.findStatic(ReflectionRedirectHook.class, "adapter$unreflect", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, MethodHandles.Lookup.class, Method.class));
            MH_adapter$unreflectSpecial = lookup.findStatic(ReflectionRedirectHook.class, "adapter$unreflectSpecial", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, MethodHandles.Lookup.class, Method.class, Class.class));
            MH_adapter$unreflectConstructor = lookup.findStatic(ReflectionRedirectHook.class, "adapter$unreflectConstructor", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, MethodHandles.Lookup.class, Constructor.class));
            MH_adapter$unreflectField = lookup.findStatic(ReflectionRedirectHook.class, "adapter$unreflectField", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, boolean.class, MethodHandles.Lookup.class, Field.class));
            MH_adapter$unreflectVarHandle = lookup.findStatic(ReflectionRedirectHook.class, "adapter$unreflectVarHandle", MethodType.methodType(VarHandle.class, SandboxRuntime.class, MethodHandles.Lookup.class, Field.class));
            MH_adapter$Field$getOrSet = lookup.findStatic(ReflectionRedirectHook.class, "adapter$Field$getOrSet", MethodType.methodType(MethodHandle.class, SandboxRuntime.class, MethodHandles.Lookup.class, boolean.class, Class.class, Field.class));
            MH_adapter$findVarHandle = lookup.findStatic(ReflectionRedirectHook.class, "adapter$findVarHandle", MethodType.methodType(VarHandle.class, SandboxRuntime.class, boolean.class, MethodHandles.Lookup.class, Class.class, String.class, Class.class));
            MH_adapter$revealDirect = lookup.findStatic(ReflectionRedirectHook.class, "adapter$revealDirect", MethodType.methodType(MethodHandleInfo.class, SandboxRuntime.class, MethodHandles.Lookup.class, MethodHandle.class));
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static MethodHandle adapter$findConstructor(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> target, MethodType desc) throws Throwable {
        return runtime.interpretInvoke(
                caller, target, "<init>", desc.changeReturnType(target), MethodHandleInfo.REF_newInvokeSpecial,
                new RuntimeResolvationInfo(desc, null, null)
        ).dynamicInvoker();
    }

    private static MethodHandle adapter$findVirtual(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> target, String name, MethodType desc) throws Throwable {
        return runtime.interpretInvoke(
                caller, target, name, desc.insertParameterTypes(0, target), MethodHandleInfo.REF_invokeVirtual,
                new RuntimeResolvationInfo(desc, null, null)
        ).dynamicInvoker();
    }

    private static MethodHandle adapter$findStatic(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> target, String name, MethodType desc) throws Throwable {
        return runtime.interpretInvoke(
                caller, target, name, desc, MethodHandleInfo.REF_invokeStatic,
                new RuntimeResolvationInfo(desc, null, null)
        ).dynamicInvoker();
    }

    private static MethodHandle adapter$findSpecial(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> target, String name, MethodType desc, Class<?> specialCaller) throws Throwable {
        return runtime.interpretInvoke(
                caller, target, name, desc.insertParameterTypes(0, caller.lookupClass()), MethodHandleInfo.REF_invokeSpecial,
                new RuntimeResolvationInfo(desc, null, specialCaller)
        ).dynamicInvoker();
    }

    private static MethodHandle adapter$findGetterSetter(SandboxRuntime runtime, int refType, MethodHandles.Lookup caller, Class<?> target, String name, Class<?> type) throws Throwable {
        return switch (refType) {
            case MethodHandleInfo.REF_getField -> runtime.interpretInvoke(
                    caller, target, name, MethodType.methodType(type, target), refType,
                    new RuntimeResolvationInfo(type, null, null)
            ).dynamicInvoker();
            case MethodHandleInfo.REF_putField -> runtime.interpretInvoke(
                    caller, target, name, MethodType.methodType(void.class, target, type), refType,
                    new RuntimeResolvationInfo(type, null, null)
            ).dynamicInvoker();

            case MethodHandleInfo.REF_getStatic -> runtime.interpretInvoke(
                    caller, target, name, MethodType.methodType(type), refType,
                    new RuntimeResolvationInfo(type, null, null)
            ).dynamicInvoker();
            case MethodHandleInfo.REF_putStatic -> runtime.interpretInvoke(
                    caller, target, name, MethodType.methodType(void.class, type), refType,
                    new RuntimeResolvationInfo(type, null, null)
            ).dynamicInvoker();

            default -> throw new AssertionError();
        };
    }

    private static VarHandle adapter$findVarHandle(SandboxRuntime runtime, boolean isStatic, MethodHandles.Lookup caller, Class<?> target, String name, Class<?> type) throws Throwable {
        return (VarHandle) runtime.interpretInvoke(
                caller, target, name, MethodType.methodType(type),
                isStatic ? InvokeHelper.EXREF_varhandleStatic : InvokeHelper.EXREF_varhandleField,
                new RuntimeResolvationInfo(type, null, null)
        ).dynamicInvoker().invoke();
    }

    private static MethodHandle adapter$unreflect(SandboxRuntime runtime, MethodHandles.Lookup caller, Method method) throws Throwable {
        if (Modifier.isStatic(method.getModifiers())) {
            return runtime.interpretInvoke(
                    caller, method.getDeclaringClass(), method.getName(),
                    MethodType.methodType(
                            method.getReturnType(), method.getParameterTypes()
                    ),
                    MethodHandleInfo.REF_invokeStatic,
                    new RuntimeResolvationInfo(
                            MethodType.methodType(
                                    method.getReturnType(), method.getParameterTypes()
                            ), method, null
                    )
            ).dynamicInvoker();
        } else {
            return runtime.interpretInvoke(
                    caller, method.getDeclaringClass(), method.getName(),
                    MethodType.methodType(
                            method.getReturnType(), method.getDeclaringClass(), method.getParameterTypes()
                    ),
                    MethodHandleInfo.REF_invokeVirtual,
                    new RuntimeResolvationInfo(
                            MethodType.methodType(
                                    method.getReturnType(), method.getParameterTypes()
                            ), method, null
                    )
            ).dynamicInvoker();
        }
    }

    private static MethodHandle adapter$unreflectSpecial(SandboxRuntime runtime, MethodHandles.Lookup caller, Method method, Class<?> specialCaller) throws Throwable {
        return runtime.interpretInvoke(
                caller, method.getDeclaringClass(), method.getName(),
                MethodType.methodType(
                        method.getReturnType(), method.getDeclaringClass(), method.getParameterTypes()
                ),
                MethodHandleInfo.REF_invokeSpecial,
                new RuntimeResolvationInfo(
                        MethodType.methodType(
                                method.getReturnType(), method.getParameterTypes()
                        ), method, specialCaller
                )
        ).dynamicInvoker();
    }

    private static MethodHandle adapter$unreflectConstructor(SandboxRuntime runtime, MethodHandles.Lookup caller, Constructor<?> constructor) throws Throwable {
        return runtime.interpretInvoke(
                caller, constructor.getDeclaringClass(), "_",
                MethodType.methodType(
                        constructor.getDeclaringClass(), constructor.getParameterTypes()
                ),
                MethodHandleInfo.REF_newInvokeSpecial,
                new RuntimeResolvationInfo(
                        MethodType.methodType(
                                void.class, constructor.getParameterTypes()
                        ), constructor, null
                )
        ).dynamicInvoker();
    }

    private static MethodHandle adapter$unreflectField(SandboxRuntime runtime, boolean getter, MethodHandles.Lookup caller, Field field) throws Throwable {
        if (getter) {
            if (Modifier.isStatic(field.getModifiers())) {
                return runtime.interpretInvoke(
                        caller, field.getDeclaringClass(), field.getName(),
                        MethodType.methodType(field.getType()),
                        MethodHandleInfo.REF_getStatic,
                        new RuntimeResolvationInfo(field.getType(), field, null)
                ).dynamicInvoker();
            } else {
                return runtime.interpretInvoke(
                        caller, field.getDeclaringClass(), field.getName(),
                        MethodType.methodType(field.getType(), field.getDeclaringClass()),
                        MethodHandleInfo.REF_getField,
                        new RuntimeResolvationInfo(field.getType(), field, null)
                ).dynamicInvoker();
            }
        } else {
            if (Modifier.isStatic(field.getModifiers())) {
                return runtime.interpretInvoke(
                        caller, field.getDeclaringClass(), field.getName(),
                        MethodType.methodType(void.class, field.getType()),
                        MethodHandleInfo.REF_putStatic,
                        new RuntimeResolvationInfo(field.getType(), field, null)
                ).dynamicInvoker();
            } else {
                return runtime.interpretInvoke(
                        caller, field.getDeclaringClass(), field.getName(),
                        MethodType.methodType(void.class, field.getDeclaringClass(), field.getType()),
                        MethodHandleInfo.REF_putField,
                        new RuntimeResolvationInfo(field.getType(), field, null)
                ).dynamicInvoker();
            }
        }
    }

    private static VarHandle adapter$unreflectVarHandle(SandboxRuntime runtime, MethodHandles.Lookup caller, Field field) throws Throwable {
        return (VarHandle) runtime.interpretInvoke(
                caller,
                field.getDeclaringClass(),
                field.getName(),
                MethodType.methodType(field.getType()),
                InvokeHelper.EXREF_varhandleField,
                new RuntimeResolvationInfo(field.getType(), field, null)
        ).dynamicInvoker().invoke();
    }

    private static Object adapter$methodInvoke(SandboxRuntime runtime, MethodHandles.Lookup caller, Method method, Object thiz, Object[] args) throws Throwable {
        Objects.requireNonNull(method);

        var cachePool = MethodUnreflectCachePool.unreflectMethodCache.get(caller.lookupClass());
        var cachedHandle = cachePool.get(method);
        if (cachedHandle != null) {
            return cachedHandle.invoke(thiz, args);
        }

        cachedHandle = adapter$unreflect(runtime, caller, method);
        if (Modifier.isStatic(method.getModifiers())) {
            cachedHandle = cachedHandle.asSpreader(Object[].class, method.getParameterTypes().length);
            cachedHandle = MethodHandles.dropArguments(cachedHandle, 0, Object.class);
        } else {
            cachedHandle = cachedHandle.asSpreader(1, Object[].class, method.getParameterTypes().length);
        }

        Object result = cachedHandle.invoke(thiz, args);
        cachePool.put(method, cachedHandle);
        return result;
    }

    private static Object adapter$constructorInvoke(SandboxRuntime runtime, MethodHandles.Lookup caller, Constructor<?> constructor, Object[] args) throws Throwable {
        Objects.requireNonNull(constructor);

        var cachePool = MethodUnreflectCachePool.unreflectMethodCache.get(caller.lookupClass());
        var cachedHandle = cachePool.get(constructor);
        if (cachedHandle != null) {
            return cachedHandle.invoke(args);
        }

        cachedHandle = adapter$unreflectConstructor(runtime, caller, constructor)
                .asSpreader(Object[].class, constructor.getParameterTypes().length);

        cachePool.put(constructor, cachedHandle);

        return cachedHandle.invoke(args);
    }

    private static Object adapter$classNewInstance(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> type) throws Throwable {
        Objects.requireNonNull(type);

        return adapter$unreflectConstructor(runtime, caller, type.getDeclaredConstructor()).invoke();
    }

    private static MethodHandleInfo adapter$revealDirect(SandboxRuntime runtime, MethodHandles.Lookup caller, MethodHandle request) throws Throwable {
        MethodHandle faked;
        try {
            faked = runtime.reflectionCache.getFakedRealHandle(request);
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }

        return caller.revealDirect(faked);
    }


    private static MethodHandle adapter$Field$getOrSet(SandboxRuntime runtime, MethodHandles.Lookup caller, boolean getter, Class<?> resultType, Field field) throws Throwable {
        Objects.requireNonNull(field);

        if (getter) {
            // (Object): ?
            var castType = MethodType.methodType(resultType, Object.class);

            var cachePool = MethodUnreflectCachePool.unreflectFieldGetterCache.get(caller.lookupClass());
            var cachedHandle = cachePool.get(field);
            if (cachedHandle != null) {
                return cachedHandle.asType(castType);
            }

            cachedHandle = adapter$unreflectField(runtime, true, caller, field);
            if (Modifier.isStatic(field.getModifiers())) {
                cachedHandle = MethodHandles.dropArguments(cachedHandle, 0, Object.class);
            }

            cachePool.put(field, cachedHandle);
            return cachedHandle.asType(castType);
        } else {
            // (Object, ?)V
            var castType = MethodType.methodType(void.class, Object.class, resultType);

            var cachePool = MethodUnreflectCachePool.unreflectFieldSetterCache.get(caller.lookupClass());
            var cachedHandle = cachePool.get(field);
            if (cachedHandle != null) {
                return cachedHandle.asType(castType);
            }

            cachedHandle = adapter$unreflectField(runtime, false, caller, field);
            if (Modifier.isStatic(field.getModifiers())) {
                cachedHandle = MethodHandles.dropArguments(cachedHandle, 0, Object.class);
            }

            cachePool.put(field, cachedHandle);
            return cachedHandle.asType(castType);
        }
    }

    @Override
    public CallSite interpretInvoke(
            SandboxRuntime runtime, MethodHandles.Lookup caller,
            Class<?> owner, String methodName, MethodType desc,
            int refType, RuntimeResolvationInfo callInfo
    ) throws Throwable {
        var result = interpretInvoke0(runtime, caller, owner, methodName, desc, refType, callInfo);
        if (result != null && callInfo != null) {
            runtime.reflectionCache.pushFakedSource(result,
                    InvokeHelper.resolveMethodHandle(caller, owner, methodName, desc, refType, callInfo)
            );
        }

        if (result != null) return new ConstantCallSite(result);
        return null;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public MethodHandle interpretInvoke0(
            SandboxRuntime runtime, MethodHandles.Lookup caller,
            Class<?> owner, String methodName, MethodType desc,
            int refType, RuntimeResolvationInfo callInfo
    ) throws Throwable {
        if (owner == MethodHandles.Lookup.class) {
            var cache = getCache(runtime);
            switch (methodName) {

                // invokes
                case "findStatic" -> {
                    // Lookup.(Class<?>, String, MethodType)MethodHandle
                    if (cache.lookup$findStatic != null) return cache.lookup$findStatic;

                    return cache.lookup$findStatic = MH_adapter$findStatic.bindTo(runtime);
                }
                case "findVirtual" -> {
                    // Lookup.(Class<?>, String name, MethodType)MethodHandle
                    if (cache.lookup$findVirtual != null) return cache.lookup$findVirtual;

                    return cache.lookup$findVirtual = MH_adapter$findVirtual.bindTo(runtime);
                }
                case "findConstructor" -> {
                    // Lookup.(Class<?>, MethodType)MethodHandle
                    if (cache.lookup$findConstructor != null) return cache.lookup$findConstructor;

                    return cache.lookup$findConstructor = MH_adapter$findConstructor.bindTo(runtime);
                }
                case "findSpecial" -> {
                    // Lookup.(Class<?>, String, MethodType, Class<?>)MethodHandle
                    if (cache.lookup$findSpecial != null) return cache.lookup$findSpecial;

                    return cache.lookup$findSpecial = MH_adapter$findSpecial.bindTo(runtime);
                }

                // fields
                case "findGetter" -> {
                    if (cache.lookup$findGetter != null) return cache.lookup$findGetter;

                    return cache.lookup$findGetter = MH_adapter$findGetterSetter.bindTo(runtime).bindTo(MethodHandleInfo.REF_getField);
                }
                case "findSetter" -> {
                    if (cache.lookup$findSetter != null) return cache.lookup$findSetter;

                    return cache.lookup$findSetter = MH_adapter$findGetterSetter.bindTo(runtime).bindTo(MethodHandleInfo.REF_putField);
                }
                case "findStaticGetter" -> {
                    if (cache.lookup$findStaticGetter != null) return cache.lookup$findStaticGetter;

                    return cache.lookup$findStaticGetter = MH_adapter$findGetterSetter.bindTo(runtime).bindTo(MethodHandleInfo.REF_getStatic);
                }
                case "findStaticSetter" -> {
                    if (cache.lookup$findStaticSetter != null) return cache.lookup$findStaticSetter;

                    return cache.lookup$findStaticSetter = MH_adapter$findGetterSetter.bindTo(runtime).bindTo(MethodHandleInfo.REF_putStatic);
                }


                case "findVarHandle" -> {
                    if (cache.lookup$findVarHandle != null) return cache.lookup$findVarHandle;

                    return cache.lookup$findVarHandle = MethodHandles.insertArguments(MH_adapter$findVarHandle, 0, runtime, false);
                }
                case "findStaticVarHandle" -> {
                    if (cache.lookup$findStaticVarHandle != null) return cache.lookup$findStaticVarHandle;

                    return cache.lookup$findStaticVarHandle = MethodHandles.insertArguments(MH_adapter$findVarHandle, 0, runtime, true);
                }


                case "unreflect" -> {
                    if (cache.lookup$unreflect != null) return cache.lookup$unreflect;

                    return cache.lookup$unreflect = MH_adapter$unreflect.bindTo(runtime);
                }
                case "unreflectSpecial" -> {
                    if (cache.lookup$unreflectSpecial != null) return cache.lookup$unreflectSpecial;

                    return cache.lookup$unreflectSpecial = MH_adapter$unreflectSpecial.bindTo(runtime);
                }
                case "unreflectConstructor" -> {
                    if (cache.lookup$unreflectConstructor != null) return cache.lookup$unreflectConstructor;

                    return cache.lookup$unreflectConstructor = MH_adapter$unreflectConstructor.bindTo(runtime);
                }
                case "unreflectGetter" -> {
                    if (cache.lookup$unreflectGetter != null) return cache.lookup$unreflectGetter;

                    return cache.lookup$unreflectGetter = MethodHandles.insertArguments(MH_adapter$unreflectField, 0, runtime, true);
                }
                case "unreflectSetter" -> {
                    if (cache.lookup$unreflectSetter != null) return cache.lookup$unreflectSetter;
                    return cache.lookup$unreflectSetter = MethodHandles.insertArguments(MH_adapter$unreflectField, 0, runtime, false);
                }
                case "unreflectVarHandle" -> {
                    if (cache.lookup$unreflectVarHandle != null) return cache.lookup$unreflectVarHandle;

                    return cache.lookup$unreflectVarHandle = MH_adapter$unreflectVarHandle.bindTo(runtime);
                }
                case "revealDirect" -> {
                    if (cache.lookup$revealDirect != null) return cache.lookup$revealDirect;
                    return cache.lookup$revealDirect = MH_adapter$revealDirect.bindTo(runtime);
                }
            }
        }

        if (owner == Method.class) {
            var cache = getCache(runtime);
            switch (methodName) {
                case "invoke" -> {
                    // Method.(Object, Object[])
                    if (cache.method$invoke != null) return cache.method$invoke;
                    return cache.method$invoke = MethodHandles.insertArguments(MH_adapter$methodInvoke, 0, runtime, caller);
                }
            }
        }
        if (owner == Constructor.class) {
            var cache = getCache(runtime);
            switch (methodName) {
                case "newInstance" -> {
                    // Constructor.(Object[])
                    if (cache.constructor$newInstance != null) return cache.constructor$newInstance;
                    return cache.constructor$newInstance = MethodHandles.insertArguments(MH_adapter$constructorInvoke, 0, runtime, caller);
                }
            }
        }
        if (owner == Class.class) {
            var cache = getCache(runtime);
            switch (methodName) {
                case "newInstance" -> {
                    if (cache.class$newInstance != null) return cache.class$newInstance;
                    return cache.class$newInstance = MethodHandles.insertArguments(MH_adapter$classNewInstance, 0, runtime, caller);
                }
            }
        }
        if (owner == Field.class) {
            switch (methodName) {
                case "get", "getBoolean", "getByte", "getChar", "getShort", "getInt", "getLong", "getFloat",
                     "getDouble" -> {

                    // Field.(Object)
                    var invoker = MethodHandles.invoker(MethodType.methodType(desc.returnType(), Object.class));
                    var mapper = MethodHandles.insertArguments(
                            MH_adapter$Field$getOrSet,
                            0,
                            runtime, caller, true, desc.returnType()
                    );

                    return MethodHandles.filterArguments(
                            invoker,
                            0,
                            mapper
                    );
                }
                case "set", "setBoolean", "setByte", "setChar", "setShort", "setInt", "setLong", "setFloat",
                     "setDouble" -> {
                    // Field.(Object, Value)

                    var invoker = MethodHandles.invoker(MethodType.methodType(void.class, Object.class, desc.lastParameterType()));
                    var mapper = MethodHandles.insertArguments(
                            MH_adapter$Field$getOrSet,
                            0,
                            runtime, caller, false, desc.lastParameterType()
                    );

                    return MethodHandles.filterArguments(
                            invoker,
                            0,
                            mapper
                    );
                }
            }
        }

        return null;
    }


    private static final class MethodUnreflectCachePool {
        private static final ClassValue<Map<Member, MethodHandle>> unreflectMethodCache = new ClassValue<>() {
            @Override
            protected Map<Member, MethodHandle> computeValue(@NotNull Class<?> type) {
                return Collections.synchronizedMap(new WeakHashMap<>());
            }
        };
        private static final ClassValue<Map<Field, MethodHandle>> unreflectFieldGetterCache = new ClassValue<>() {
            @Override
            protected Map<Field, MethodHandle> computeValue(@NotNull Class<?> type) {
                return Collections.synchronizedMap(new WeakHashMap<>());
            }
        };
        private static final ClassValue<Map<Field, MethodHandle>> unreflectFieldSetterCache = new ClassValue<>() {
            @Override
            protected Map<Field, MethodHandle> computeValue(@NotNull Class<?> type) {
                return Collections.synchronizedMap(new WeakHashMap<>());
            }
        };
    }

    private static final Symbol SYMBOL_SANDBOX_HANDLES_CACHE = new Symbol("SandboxHandlesCache");

    private static SandboxHandlesCache getCache(SandboxRuntime runtime) {
        final var runtimeCache = runtime.runtimeCache;
        var result = runtimeCache.get(SYMBOL_SANDBOX_HANDLES_CACHE);
        if (result != null) return (SandboxHandlesCache) result;

        result = new SandboxHandlesCache();
        var cached = runtimeCache.putIfAbsent(SYMBOL_SANDBOX_HANDLES_CACHE, result);
        if (cached != null) return (SandboxHandlesCache) cached;
        return (SandboxHandlesCache) result;
    }

    private static final class SandboxHandlesCache {
        MethodHandle lookup$findStatic;
        MethodHandle lookup$findVirtual;
        MethodHandle lookup$findConstructor;
        MethodHandle lookup$findSpecial;
        MethodHandle lookup$findGetter;
        MethodHandle lookup$findSetter;
        MethodHandle lookup$findStaticGetter;
        MethodHandle lookup$findStaticSetter;
        MethodHandle lookup$findVarHandle;
        MethodHandle lookup$findStaticVarHandle;
        MethodHandle lookup$unreflect;
        MethodHandle lookup$unreflectSpecial;
        MethodHandle lookup$unreflectConstructor;
        MethodHandle lookup$unreflectGetter;
        MethodHandle lookup$unreflectSetter;
        MethodHandle lookup$unreflectVarHandle;
        MethodHandle lookup$revealDirect;

        MethodHandle method$invoke;
        MethodHandle constructor$newInstance;
        MethodHandle class$newInstance;
    }
}
