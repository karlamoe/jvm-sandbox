package moe.karla.jvmsandbox.runtime.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static java.lang.invoke.MethodHandleInfo.*;

public class InvokeHelper {
    private static final int prefix_EXREF = 0x1344 << 12;

    public static final int EXREF_varhandleField = prefix_EXREF | 10;
    public static final int EXREF_varhandleStatic = prefix_EXREF | 11;

    public static MethodHandle wrapVarHandle(VarHandle varHandle) {
        return MethodHandles.constant(VarHandle.class, varHandle);
    }

    @SuppressWarnings("DataFlowIssue")
    public static MethodHandle resolveMethodHandle(
            MethodHandles.Lookup lookup,
            Class<?> owner, String methodName, MethodType desc,
            int refType, RuntimeResolvationInfo callInfo
    ) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException {

        if (callInfo != null) {
            // forwarded from reflection
            switch (refType) {
                case REF_invokeStatic -> {
                    if (callInfo.relatedMember() != null) {
                        return lookup.unreflect((Method) callInfo.relatedMember());
                    }

                    return lookup.findStatic(owner, methodName, (MethodType) callInfo.typeDescriptor());
                }
                case REF_invokeInterface, REF_invokeVirtual -> {
                    if (callInfo.relatedMember() != null) {
                        return lookup.unreflect((Method) callInfo.relatedMember());
                    }

                    return lookup.findVirtual(owner, methodName, (MethodType) callInfo.typeDescriptor());
                }
                case REF_invokeSpecial -> {
                    if (callInfo.relatedMember() != null) {
                        return lookup.unreflectSpecial((Method) callInfo.relatedMember(), callInfo.specialCaller());
                    }

                    return lookup.findSpecial(owner, methodName, (MethodType) callInfo.typeDescriptor(), callInfo.specialCaller());
                }


                case REF_getField -> {
                    if (callInfo.relatedMember() != null) {
                        return lookup.unreflectGetter((Field) callInfo.relatedMember());
                    }
                    return lookup.findGetter(owner, methodName, (Class<?>) callInfo.typeDescriptor());
                }
                case REF_getStatic -> {
                    if (callInfo.relatedMember() != null) {
                        return lookup.unreflectGetter((Field) callInfo.relatedMember());
                    }
                    return lookup.findStaticGetter(owner, methodName, (Class<?>) callInfo.typeDescriptor());
                }
                case REF_putField -> {
                    if (callInfo.relatedMember() != null) {
                        return lookup.unreflectSetter((Field) callInfo.relatedMember());
                    }
                    return lookup.findSetter(owner, methodName, (Class<?>) callInfo.typeDescriptor());
                }
                case REF_putStatic -> {
                    if (callInfo.relatedMember() != null) {
                        return lookup.unreflectSetter((Field) callInfo.relatedMember());
                    }
                    return lookup.findStaticSetter(owner, methodName, (Class<?>) callInfo.typeDescriptor());
                }


                case REF_newInvokeSpecial -> {
                    if (callInfo.relatedMember() != null) {
                        return lookup.unreflectConstructor((Constructor<?>) callInfo.relatedMember());
                    }
                    return lookup.findConstructor(owner, (MethodType) callInfo.typeDescriptor());
                }


                case EXREF_varhandleField -> {
                    if (callInfo.relatedMember() != null) {
                        return wrapVarHandle(lookup.unreflectVarHandle((Field) callInfo.relatedMember()));
                    }
                    return wrapVarHandle(lookup.findVarHandle(owner, methodName, (Class<?>) callInfo.typeDescriptor()));
                }
                case EXREF_varhandleStatic -> {
                    if (callInfo.relatedMember() != null) {
                        return wrapVarHandle(lookup.unreflectVarHandle((Field) callInfo.relatedMember()));
                    }
                    return wrapVarHandle(lookup.findStaticVarHandle(owner, methodName, (Class<?>) callInfo.typeDescriptor()));
                }
            }
        }

        return switch (refType) {
            case REF_getField -> lookup.findGetter(owner, methodName, desc.returnType());
            case REF_putField -> lookup.findSetter(owner, methodName, desc.parameterType(1));
            case REF_getStatic -> lookup.findStaticGetter(owner, methodName, desc.returnType());
            case REF_putStatic -> lookup.findStaticSetter(owner, methodName, desc.parameterType(0));

            case REF_invokeStatic -> lookup.findStatic(owner, methodName, desc);
            case REF_invokeSpecial ->
                    lookup.findSpecial(owner, methodName, desc.dropParameterTypes(0, 1), lookup.lookupClass());
            case REF_invokeInterface, REF_invokeVirtual ->
                    lookup.findVirtual(owner, methodName, desc.dropParameterTypes(0, 1));

            case REF_newInvokeSpecial -> lookup.findConstructor(owner, desc.changeReturnType(void.class));

            default -> throw new IllegalArgumentException(
                    "Unknown refType: " + refType + ": " + owner + '.' + methodName + desc
            );
        };
    }
}
