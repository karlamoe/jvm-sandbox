package tests;

import org.junit.jupiter.api.Test;
import util.InstructedTest;

public class SimpleTest extends InstructedTest {
    public static class TargetClass implements Runnable {
        @Override
        public void run() {
            System.out.println("Hello World");
            Thread.dumpStack();
        }
    }

    @Test
    void run() throws Throwable {
        ((Runnable) targetClass.newInstance()).run();
    }
}
