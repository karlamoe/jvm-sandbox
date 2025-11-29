package tests;

import moe.karla.jvmsandbox.instructment.generator.HookWrapperInterpreter;
import moe.karla.jvmsandbox.transformer.context.ApplicationTransformContext;
import util.InstructedTest;

public abstract class InvokeDynamicFakerTest extends InstructedTest {
    public static class InterfaceTestCase extends InvokeDynamicFakerTest {
        public static interface TargetClass {
            public static void run() {
                Thread.dumpStack();
            }
        }
    }

    public static class NormalClassTestCase extends InvokeDynamicFakerTest {
        public static class TargetClass {
            public static void run() {
                Thread.dumpStack();
            }
        }
    }

    public static class EnumTestCase extends InvokeDynamicFakerTest {
        public static enum TargetClass {
            A, B, C, D;

            public static void run() {
                Thread.dumpStack();
            }
        }
    }

    @Override
    protected void setupContext(ApplicationTransformContext context, String targetClass) {
        context.interpreter = new HookWrapperInterpreter(targetClass, true);
    }
}
