package moe.karla.jvmsandbox.transformer.transformers;

import moe.karla.jvmsandbox.transformer.TransformContext;
import moe.karla.jvmsandbox.transformer.Transformer;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;

public class AllocPostProcessTransformer extends Transformer {
    @Override
    public ClassNode transform(ClassNode node, TransformContext context) throws Throwable {
        for (var method : new ArrayList<>(node.methods)) {
            if (method.instructions == null) continue;

            var iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                var insn = iterator.next();

                if (insn instanceof MethodInsnNode met) {
                    if (met.name.equals(AllocPreProcessTransformer.NAME_CONSTRUCTOR_SUPER_INVOKE)) {
                        context.interpreter.interpretSuperConstructorCall(node, method, context, iterator, met);
                    } else if (met.name.equals(AllocPreProcessTransformer.NAME_NEW_OBJECT)) {
                        context.interpreter.interpretObjectNew(node, method, context, iterator, met);
                    } else {
                        context.interpreter.interpretMethodCall(node, method, context, iterator, met);
                    }
                } else if (insn instanceof FieldInsnNode field) {
                    context.interpreter.interpretFieldCall(node, method, context, iterator, field);
                } else if (insn instanceof InvokeDynamicInsnNode dynamicInsnNode) {
                    context.interpreter.interpretDynamicCall(node, method, context, iterator, dynamicInsnNode);
                } else if (insn instanceof LdcInsnNode ldcInsnNode) {
                    context.interpreter.interpretLdcInsn(node, method, context, iterator, ldcInsnNode);
                }
            }
        }
        return node;
    }
}
