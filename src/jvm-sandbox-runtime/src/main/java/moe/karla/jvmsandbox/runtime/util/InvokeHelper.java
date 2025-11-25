package moe.karla.jvmsandbox.runtime.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class InvokeHelper {
    public static MethodHandle resolveMethodHandle(
            MethodHandles.Lookup lookup,
            Class<?> owner,
            String methodName,
            MethodType desc,
            int refType
    ) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException {
        return switch (refType) {
            case MethodHandleInfo.REF_getField -> lookup.findGetter(owner, methodName, desc.returnType());
            case MethodHandleInfo.REF_putField -> lookup.findSetter(owner, methodName, desc.parameterType(1));
            case MethodHandleInfo.REF_getStatic -> lookup.findStaticGetter(owner, methodName, desc.returnType());
            case MethodHandleInfo.REF_putStatic -> lookup.findStaticSetter(owner, methodName, desc.parameterType(0));

            case MethodHandleInfo.REF_invokeStatic -> lookup.findStatic(owner, methodName, desc);
            case MethodHandleInfo.REF_invokeSpecial ->
                    lookup.findSpecial(owner, methodName, desc.dropParameterTypes(0, 1), lookup.lookupClass());
            case MethodHandleInfo.REF_invokeInterface, MethodHandleInfo.REF_invokeVirtual ->
                    lookup.findVirtual(owner, methodName, desc.dropParameterTypes(0, 1));

            case MethodHandleInfo.REF_newInvokeSpecial ->
                    lookup.findConstructor(owner, desc.changeReturnType(void.class));

            default -> throw new IllegalArgumentException(
                    "Unknown refType: " + refType + ": " + owner + '.' + methodName + desc
            );
        };
    }
}
