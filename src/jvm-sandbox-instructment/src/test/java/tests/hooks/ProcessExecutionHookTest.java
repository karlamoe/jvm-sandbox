package tests.hooks;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.helper.ProcessExecutionHook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import util.InstructedTest;

import java.lang.invoke.MethodHandles;

@EnabledOnOs(OS.WINDOWS)
public abstract class ProcessExecutionHookTest extends InstructedTest {
    boolean newProcessStarted;


    @Override
    protected void setup(SandboxRuntime runtime) {
        runtime.addHook(new ProcessExecutionHook() {
            @Override
            protected Process hookProcessBuilderStart(MethodHandles.Lookup caller, ProcessBuilder builder) throws Throwable {
                System.out.println("Starting new process: " + builder.command());
                newProcessStarted = true;
                return builder.inheritIO().start();
            }
        });
    }


    @Test
    void requireProcessStarted() {
        Assertions.assertTrue(newProcessStarted);
    }

    public static class ProcessBuilderTest extends ProcessExecutionHookTest {
        public static class TargetClass {
            public static void run() throws Throwable {
                new ProcessBuilder().command("cmd", "/c", "echo", "hello world").start();
            }
        }
    }

    public static class RuntimeExec1Test extends ProcessExecutionHookTest {
        public static class TargetClass {
            public static void run() throws Throwable {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "echo", "hello world"});
            }
        }
    }

    public static class RuntimeExec2Test extends ProcessExecutionHookTest {
        public static class TargetClass {
            public static void run() throws Throwable {
                Runtime.getRuntime().exec("cmd /c echo hello world");
            }
        }
    }
}
