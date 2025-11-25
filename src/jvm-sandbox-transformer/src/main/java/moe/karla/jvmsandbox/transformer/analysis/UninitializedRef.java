package moe.karla.jvmsandbox.transformer.analysis;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;

public class UninitializedRef extends BasicValue {
    public UninitializedRef(Type type) {
        super(type);
    }
}
