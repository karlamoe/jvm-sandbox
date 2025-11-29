package moe.karla.jvmsandbox.transformer.context;

import org.objectweb.asm.tree.ClassNode;

public class ClassTransformContext extends TransformContext {
    public final ApplicationTransformContext parent;
    public final ClassNode klass;

    public ClassTransformContext(ApplicationTransformContext parent, ClassNode klass) {
        this.parent = parent;
        this.klass = klass;
    }
}
