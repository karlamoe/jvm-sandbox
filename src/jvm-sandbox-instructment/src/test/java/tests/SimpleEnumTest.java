package tests;

import util.InstructedTest;

public class SimpleEnumTest extends InstructedTest {
    public static enum TargetClass {
        A, B, C, D;

        public static void run() {
            Thread.dumpStack();
        }
    }
}
