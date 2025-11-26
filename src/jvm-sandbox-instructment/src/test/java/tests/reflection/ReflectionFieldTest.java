package tests.reflection;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.helper.ReflectionRedirectHook;
import moe.karla.jvmsandbox.runtime.hooks.InvocationHook;
import moe.karla.jvmsandbox.runtime.util.RuntimeResolvationInfo;
import org.junit.jupiter.api.Assertions;
import util.InstructedTest;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ReflectionFieldTest extends InstructedTest {
    public static class TargetClass implements Runnable {
        private int a;

        @Override
        public void run() {
            try {
                getClass().getDeclaredField("a").set(this, 666);
                System.out.println(a);
                Assertions.assertEquals(666, a);
                Assertions.assertEquals(666, getClass().getDeclaredField("a").getInt(this));
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
                System.out.println("LINKAGE: " + owner + " " + methodName + " " + desc + " " + refType);
                return null;
            }
        });
    }
}
