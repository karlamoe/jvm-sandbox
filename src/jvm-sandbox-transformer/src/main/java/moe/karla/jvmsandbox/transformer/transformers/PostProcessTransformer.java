package moe.karla.jvmsandbox.transformer.transformers;

import moe.karla.jvmsandbox.transformer.Transformer;
import moe.karla.jvmsandbox.transformer.context.ApplicationTransformContext;
import moe.karla.jvmsandbox.transformer.context.ClassTransformContext;
import moe.karla.jvmsandbox.transformer.context.MethodTransformContext;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;

public class PostProcessTransformer extends Transformer {
    @Override
    public ClassNode transform(ClassNode node, ApplicationTransformContext context) throws Throwable {
        var classContext = new ClassTransformContext(context, node);
        for (var method : new ArrayList<>(node.methods)) {
            if (method.instructions == null) continue;

            var methodContext = new MethodTransformContext(classContext, method);

            var iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                var insn = iterator.next();

                if (insn instanceof MethodInsnNode met) {
                    if (met.name.equals(AllocPreProcessTransformer.NAME_CONSTRUCTOR_SUPER_INVOKE)) {
                        context.interpreter.interpretSuperConstructorCall(methodContext, iterator, met);
                    } else if (met.name.equals(AllocPreProcessTransformer.NAME_NEW_OBJECT)) {
                        context.interpreter.interpretObjectNew(methodContext, iterator, met);
                    } else {
                        context.interpreter.interpretMethodCall(methodContext, iterator, met);
                    }
                } else if (insn instanceof FieldInsnNode field) {
                    context.interpreter.interpretFieldCall(methodContext, iterator, field);
                } else if (insn instanceof InvokeDynamicInsnNode dynamicInsnNode) {
                    context.interpreter.interpretDynamicCall(methodContext, iterator, dynamicInsnNode);
                } else if (insn instanceof LdcInsnNode ldcInsnNode) {
                    context.interpreter.interpretLdcInsn(methodContext, iterator, ldcInsnNode);
                }
            }
        }
        return node;
    }
}
