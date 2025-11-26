package tests;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.helper.ReflectionRedirectHook;
import moe.karla.jvmsandbox.runtime.hooks.InvocationHook;
import util.InstructedTest;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

public class ConstructTest extends InstructedTest {
    public static class TestTemplate {
        public TestTemplate(Object value) {
            System.out.println(value);
        }
    }

    public static class TargetClass extends TestTemplate implements Runnable {
        public TargetClass() throws Throwable {
            super(new Object());
            Thread.dumpStack();
            Object.class.getConstructor().newInstance();
            Object.class.newInstance();
            MethodHandles.lookup().findConstructor(Object.class, MethodType.methodType(void.class)).invoke();
        }

        @Override
        public void run() {
            Thread.dumpStack();
        }
    }

    @Override
    protected void setup(SandboxRuntime runtime) {
        runtime.addHook(new ReflectionRedirectHook());
        runtime.addHook(new InvocationHook() {
            @Override
            public CallSite interpretBeforeObjectConstruct(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> target, MethodType methodType) throws Throwable {
                return new ConstantCallSite(
                        MethodHandles.lookup().findStatic(
                                        ConstructTest.class,
                                        "listen",
                                        MethodType.methodType(void.class, MethodHandles.Lookup.class, Class.class, MethodType.class, Object[].class)
                                ).bindTo(caller).bindTo(target).bindTo(methodType)
                                .asVarargsCollector(Object[].class)
                                .asType(methodType)
                );
            }
        });
    }

    private static void listen(MethodHandles.Lookup caller, Class<?> target, MethodType methodType, Object... args) {
        System.out.println(caller + " is trying to construct a new " + target + methodType + " with arguments " + Arrays.toString(args));
        Thread.dumpStack();
    }
}
