package tests;

import moe.karla.jvmsandbox.transformer.util.ASMUtil;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import util.GeneratedTest;

import java.lang.invoke.MethodHandles;


public class DynamicConstantTest extends GeneratedTest {
    public static Object classData(MethodHandles.Lookup lookup, String name, Class<?> type) throws Throwable {
        Thread.dumpStack();
        return "Test";
    }

    @Override
    protected void setup(ClassNode node, MethodVisitor mv) {
        mv.visitLdcInsn(new ConstantDynamic(
                "_",
                "Ljava/lang/Object;",
                new Handle(Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(DynamicConstantTest.class),
                        "classData",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;",
                        false
                )
        ));
        ASMUtil.printlnLast(mv);
    }
}
