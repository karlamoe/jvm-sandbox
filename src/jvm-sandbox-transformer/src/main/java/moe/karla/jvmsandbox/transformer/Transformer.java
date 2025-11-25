package moe.karla.jvmsandbox.transformer;

import org.objectweb.asm.tree.ClassNode;

public abstract class Transformer {
    public ClassNode transform(ClassNode node, TransformContext context) throws Throwable {
        return node;
    }
}
