package moe.karla.jvmsandbox.transformer.interpreter;

import moe.karla.jvmsandbox.transformer.TransformContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class TransformInterpreter {
    public void interpretObjectNew(
            ClassNode klass,
            MethodNode method,
            TransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            MethodInsnNode node
    ) {
        iterator.add(new TypeInsnNode(
                Opcodes.NEW,
                node.owner
        ));
        node.setOpcode(Opcodes.INVOKESPECIAL);
        node.name = "<init>";
        node.desc = Type.getMethodDescriptor(
                Type.VOID_TYPE,
                Type.getArgumentTypes(node.desc)
        );
    }

    public void interpretSuperConstructorCall(
            ClassNode klass,
            MethodNode method,
            TransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            MethodInsnNode node
    ) {
        node.name = "<init>";
    }

    public void interpretMethodCall(
            ClassNode klass,
            MethodNode method,
            TransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            MethodInsnNode node
    ) {
    }

    public void interpretFieldCall(
            ClassNode klass,
            MethodNode method,
            TransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            FieldInsnNode node
    ) {
    }

    public void interpretDynamicCall(
            ClassNode klass,
            MethodNode method,
            TransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            InvokeDynamicInsnNode node
    ) {

    }

    public void interpretLdcInsn(
            ClassNode klass,
            MethodNode method,
            TransformContext context,

            ListIterator<AbstractInsnNode> iterator,
            LdcInsnNode node
    ) {
    }
}
