package moe.karla.jvmsandbox.runtime.helper;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.hooks.InvocationHook;
import moe.karla.jvmsandbox.runtime.util.InvokeHelper;
import moe.karla.jvmsandbox.runtime.util.RuntimeResolvationInfo;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

public class SecurityCallBlockHook extends InvocationHook {
    protected boolean canAccess(MethodHandles.Lookup caller, Class<?> owner, String methodName, MethodType desc, int refType) {
        return true;
    }

    @Override
    public CallSite interpretInvoke(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> owner, String methodName, MethodType desc, int refType, RuntimeResolvationInfo callInfo) throws Throwable {
        if (!canAccess(caller, owner, methodName, desc, refType)) {
            throw makeNoAccess(caller, owner, methodName, desc, refType);
        }
        return null;
    }

    @Override
    public CallSite interpretInvokeDynamic(SandboxRuntime runtime, MethodHandles.Lookup caller, String methodName, MethodType desc, Class<?> metafactory, String factoryName, MethodType factoryType, Object[] args) throws Throwable {
        if (!canAccess(caller, metafactory, methodName, desc, MethodHandleInfo.REF_invokeStatic)) {
            throw makeNoAccess(caller, metafactory, factoryName, factoryType, MethodHandleInfo.REF_invokeStatic);
        }
        return null;
    }

    @Override
    public @Nullable Optional<?> interpretInvokeDynamicConstant(SandboxRuntime runtime, MethodHandles.Lookup caller, String methodName, Class<?> resultType, Class<?> metafactory, String factoryName, MethodType factoryType, Object[] args) throws Throwable {
        if (!canAccess(caller, metafactory, factoryName, factoryType, MethodHandleInfo.REF_invokeStatic)) {
            throw makeNoAccess(caller, metafactory, factoryName, factoryType, MethodHandleInfo.REF_invokeStatic);
        }
        return null;
    }

    protected String makeNoAccessMessage(MethodHandles.Lookup caller, Class<?> owner, String methodName, MethodType desc, int refType) {
        var sb = new StringBuilder();
        sb.append(caller);
        sb.append(" is not allowed to perform ");
        if (refType == InvokeHelper.EXREF_beforeConstructor) {
            sb.append("newInvokeSpecial");
        } else {
            sb.append(MethodHandleInfo.referenceKindToString(refType));
        }
        sb.append("/");
        sb.append(owner.getName());
        sb.append('.');
        sb.append(methodName);
        sb.append(':');
        sb.append(desc);

        return sb.toString();
    }

    protected Throwable makeNoAccess(MethodHandles.Lookup caller, Class<?> owner, String methodName, MethodType desc, int refType) {
        return new LinkageError(makeNoAccessMessage(caller, owner, methodName, desc, refType));
    }
}
