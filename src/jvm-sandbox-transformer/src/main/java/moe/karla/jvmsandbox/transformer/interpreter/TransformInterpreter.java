package moe.karla.jvmsandbox.transformer.interpreter;

import moe.karla.jvmsandbox.transformer.context.MethodTransformContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class TransformInterpreter {
    public void interpretObjectNew(
            MethodTransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            MethodInsnNode node
    ) throws Throwable {
        iterator.add(new TypeInsnNode(
                Opcodes.NEW,
                node.owner
        ));
        if (!node.desc.endsWith(")V")) {
            iterator.add(new InsnNode(Opcodes.DUP));
        }
        node.setOpcode(Opcodes.INVOKESPECIAL);
        node.name = "<init>";
        node.desc = Type.getMethodDescriptor(
                Type.VOID_TYPE,
                Type.getArgumentTypes(node.desc)
        );
    }

    public void interpretSuperConstructorCall(
            MethodTransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            MethodInsnNode node
    ) throws Throwable {
        node.name = "<init>";
    }

    public void interpretMethodCall(
            MethodTransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            MethodInsnNode node
    ) throws Throwable {
    }

    public void interpretFieldCall(
            MethodTransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            FieldInsnNode node
    ) throws Throwable {
    }

    public void interpretDynamicCall(
            MethodTransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            InvokeDynamicInsnNode node
    ) throws Throwable {

    }

    public void interpretLdcInsn(
            MethodTransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            LdcInsnNode node
    ) throws Throwable {
    }
}
