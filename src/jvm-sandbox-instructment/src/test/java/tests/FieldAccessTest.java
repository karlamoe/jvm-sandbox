package tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import util.InstructedTest;

public class FieldAccessTest extends InstructedTest {
    public static class TargetClass implements Runnable {
        private int a;
        private static int b;

        @Override
        public void run() {
            a++;
            b++;
            Assertions.assertEquals(a, b);
            Assertions.assertEquals(1, b);
            Assertions.assertEquals(1, a);
            a = 0;
            b = 0;
            Assertions.assertEquals(0, b);
            Assertions.assertEquals(0, a);
            System.out.println("OK");
        }
    }

    @Test
    void run() throws Throwable {
        ((Runnable) targetClass.newInstance()).run();
    }
}
