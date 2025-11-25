package tests;

import org.junit.jupiter.api.Test;
import util.InstructedTest;

import java.io.Serializable;

public class LambdaTest extends InstructedTest {
    public static class TargetClass implements Runnable {
        @Override
        public void run() {
            Runnable task = Thread::dumpStack;
            task.run();

            task = (Runnable & Serializable) Thread::dumpStack;
            task.run();
        }
    }

    @Test
    void run() throws Throwable {
        ((Runnable) targetClass.newInstance()).run();
    }
}
