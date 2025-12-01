package tests.reflection;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.helper.ReflectionRedirectHook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import util.InstructedTest;

import java.lang.reflect.InvocationTargetException;

public class InvokeReflectionThrowingTest extends InstructedTest {
    public static class TargetClass {
        private static void a(String arg) {
            throw new RuntimeException(arg);
        }

        public static void run() throws Throwable {
            TargetClass.class.getDeclaredMethod("a", String.class).invoke(null, "Test");
        }
    }

    @Override
    protected void setup(SandboxRuntime runtime) {
        runtime.addHook(new ReflectionRedirectHook());
    }

    protected void checkError(Throwable e) {
        e.printStackTrace(System.out);
        Assertions.assertInstanceOf(InvocationTargetException.class, e);

        var cause = e.getCause();
        Assertions.assertNotNull(cause);
        Assertions.assertInstanceOf(RuntimeException.class, cause);

        Assertions.assertEquals("Test", cause.getMessage());
    }

    @Test
    @Order(-4444)
    protected void $$execute() throws Throwable {
        var err = Assertions.assertThrowsExactly(InvocationTargetException.class, super::$$execute);
        checkError(err.getCause());
    }

    @Test
    protected void testNormalExecute() throws Throwable {
        var err = Assertions.assertThrowsExactly(InvocationTargetException.class, TargetClass::run);
        checkError(err);
    }

}
