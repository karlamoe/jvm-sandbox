package util;

import moe.karla.jvmsandbox.transformer.util.ASMUtil;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public abstract class GeneratedTest extends InstructedTest {
    @Override
    protected void setupTargetNode(ClassNode node) throws Throwable {
        node.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "test/GeneratedClass", null, "java/lang/Object", new String[]{"java/lang/Runnable"});
        ASMUtil.generateConstructor(node, "java/lang/Object", Opcodes.ACC_PUBLIC);

        var mv = node.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        setup(node, mv);
        mv.visitInsn(Opcodes.RETURN);
    }

    protected abstract void setup(ClassNode node, MethodVisitor mv);
}
