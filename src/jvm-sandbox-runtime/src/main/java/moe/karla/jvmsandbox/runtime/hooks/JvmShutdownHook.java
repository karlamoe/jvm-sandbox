package moe.karla.jvmsandbox.runtime.hooks;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.util.RuntimeResolvationInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class JvmShutdownHook extends FakedInvocationHook {
    @Override
    public MethodHandle interpretInvoke0(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> owner, String methodName, MethodType desc, int refType, RuntimeResolvationInfo callInfo) throws Throwable {
        if (owner == Runtime.class) {
            if (methodName.equals("exit")) {
                var mh = MethodHandles.lookup().findVirtual(JvmShutdownHook.class, "interpretShutdownJvm", MethodType.methodType(void.class, MethodHandles.Lookup.class, boolean.class, int.class));
                mh = MethodHandles.insertArguments(mh, 0, this, caller, false);
                return MethodHandles.dropArguments(mh, 0, desc.parameterType(0));
            }
            if (methodName.equals("halt")) {
                var mh = MethodHandles.lookup().findVirtual(JvmShutdownHook.class, "interpretShutdownJvm", MethodType.methodType(void.class, MethodHandles.Lookup.class, boolean.class, int.class));
                mh = MethodHandles.insertArguments(mh, 0, this, caller, true);
                return MethodHandles.dropArguments(mh, 0, desc.parameterType(0));
            }
        }
        if (owner == System.class) {
            if (methodName.equals("exit")) {
                var mh = MethodHandles.lookup().findVirtual(JvmShutdownHook.class, "interpretShutdownJvm", MethodType.methodType(void.class, MethodHandles.Lookup.class, boolean.class, int.class));
                mh = MethodHandles.insertArguments(mh, 0, this, caller, false);
                return mh;
            }
        }
        return null;
    }


    protected void interpretShutdownJvm(MethodHandles.Lookup caller, boolean halt, int code) throws Throwable {
        throw new SecurityException(caller + " is not allowed to " + (halt ? "halt" : "exit") + " the jvm with code " + code);
    }
}
