package moe.karla.jvmsandbox.transformer;

import moe.karla.jvmsandbox.transformer.context.ApplicationTransformContext;
import org.objectweb.asm.tree.ClassNode;

public abstract class Transformer {
    public ClassNode transform(ClassNode node, ApplicationTransformContext context) throws Throwable {
        return node;
    }
}
