package moe.karla.jvmsandbox.transformer.analysis;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;

public class UninitializedThis extends BasicValue {
    public UninitializedThis(Type type) {
        super(type);
    }
}
