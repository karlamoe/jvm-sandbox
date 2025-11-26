package tests.reflection;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.helper.ReflectionRedirectHook;
import moe.karla.jvmsandbox.runtime.hooks.InvocationHook;
import moe.karla.jvmsandbox.runtime.util.RuntimeResolvationInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import util.InstructedTest;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class InvokeReflectionTest extends InstructedTest {
    public static class TargetClass implements Runnable {
        @Override
        public void run() {
            try {
                MethodHandles.lookup().findStatic(Thread.class, "dumpStack", MethodType.methodType(void.class)).invoke();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    boolean triggeredDumpStackSearch;

    @Override
    protected void setup(SandboxRuntime runtime) {
        runtime.addHook(new ReflectionRedirectHook());
        runtime.addHook(new InvocationHook() {
            @Override
            public CallSite interpretInvoke(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> owner, String methodName, MethodType desc, int refType, RuntimeResolvationInfo callInfo) throws Throwable {
                if (owner == Thread.class && "dumpStack".equals(methodName)) {
                    // throw new IllegalAccessError();
                    triggeredDumpStackSearch = true;
                }
                return null;
            }
        });
    }

    @Override
    @Test
    protected void $$execute() throws Throwable {
        super.$$execute();
        Assertions.assertTrue(triggeredDumpStackSearch);
    }
}
