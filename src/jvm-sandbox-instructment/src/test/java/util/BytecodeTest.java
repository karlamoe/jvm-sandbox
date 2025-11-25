package util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public abstract class BytecodeTest extends InstructedTest {
    protected abstract byte[] dump();

    @Override
    protected void setupTargetNode(ClassNode node) throws Throwable {
        new ClassReader(dump()).accept(node, 0);
    }
}
