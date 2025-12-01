package tests.hooks;


import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.hooks.JvmShutdownHook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import util.InstructedTest;

import java.lang.reflect.InvocationTargetException;

public abstract class JvmShutdownHookTest extends InstructedTest {
    @Override
    protected void setup(SandboxRuntime runtime) {
        runtime.addHook(new JvmShutdownHook());
    }

    @Override
    @Test
    protected void $$execute() throws Throwable {
        var err = Assertions.assertThrowsExactly(InvocationTargetException.class, super::$$execute);
        Assertions.assertInstanceOf(SecurityException.class, err.getCause());
        err.printStackTrace(System.out);
    }

    public static class RuntimeExitTest extends JvmShutdownHookTest {
        public static class TargetClass {
            public static void run() {
                Runtime.getRuntime().exit(0);
            }
        }
    }

    public static class RuntimeHaltTest extends JvmShutdownHookTest {
        public static class TargetClass {
            public static void run() {
                Runtime.getRuntime().halt(0);
            }
        }
    }

    public static class SystemExitTest extends JvmShutdownHookTest {
        public static class TargetClass {
            public static void run() {
                System.exit(0);
            }
        }
    }

}
