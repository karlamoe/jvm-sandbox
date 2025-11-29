package moe.karla.jvmsandbox.transformer.context;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodTransformContext extends TransformContext {
    public final ClassTransformContext parent;
    public final MethodNode method;
    public final ClassNode klass;

    public MethodTransformContext(ClassTransformContext parent, MethodNode method) {
        this.parent = parent;
        this.method = method;
        this.klass = parent.klass;
    }
}
