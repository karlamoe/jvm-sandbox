package tests.reflection;

import tests.ConstructTest;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class FakedMethodHandleRealSourceTest extends ConstructTest {
    public static class TargetClass {
        public static void run() throws Throwable {
            Thread.dumpStack();

            var lookup = MethodHandles.lookup();
            var constructor = lookup.findConstructor(Object.class, MethodType.methodType(void.class));

            System.out.println("constructor: " + constructor);
            System.out.println("instance: " + constructor.invoke());

            System.out.println("info: " + lookup.revealDirect(constructor));
            System.out.println("info: " + lookup.revealDirect(lookup.unreflectConstructor(
                    Object.class.getConstructor()
            )));
        }
    }
}
