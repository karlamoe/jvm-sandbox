package moe.karla.jvmsandbox.transformer.util;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.function.Consumer;

public class ASMUtil {
    public static void generateConstructor(ClassVisitor visitor, String superKlass) {
        generateConstructor(visitor, superKlass, Opcodes.ACC_PRIVATE);
    }

    public static void generateConstructor(ClassVisitor visitor, String superKlass, int access) {
        MethodVisitor methodVisitor = visitor.visitMethod(
                access,
                "<init>", "()V",
                null, null
        );
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superKlass, "<init>", "()V", false);
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
    }

    public static void pushLocalsToStack(MethodVisitor mv, int slot, Type[] arguments) {
        for (var type : arguments) {
            mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), slot);
            slot += type.getSize();
        }
    }

    public static void popStackReturn(MethodVisitor mv, Type type) {
        mv.visitInsn(type.getOpcode(Opcodes.IRETURN));
    }

    public static Object[] getBSMArguments(ConstantDynamic dynamic) {
        var newBsmArgs = new Object[dynamic.getBootstrapMethodArgumentCount()];
        for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) {
            newBsmArgs[i] = dynamic.getBootstrapMethodArgument(i);
        }
        return newBsmArgs;
    }

    public static void printlnLast(MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
    }

    public static void emitMethodType(MethodVisitor mv, String currentKlass, String methodDesc) {
        mv.visitLdcInsn(methodDesc);
        mv.visitLdcInsn(Type.getObjectType(currentKlass));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false);
    }

    public static void getLookup(MethodVisitor mv) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
    }

    public static AbstractInsnNode pushInvokeDynamic(
            ClassNode klass,
            boolean noInvokeDynamic,
            String methodName, String methodDesc,
            Handle bsm, Object[] bsmArgs
    ) {
        if (!noInvokeDynamic && klass.version >= Opcodes.V1_8) {
            return new InvokeDynamicInsnNode(methodName, methodDesc, bsm, bsmArgs);
        }
        return InvokeDynamicFaker.pushInvokeDynamic(klass, methodName, methodDesc, bsm, bsmArgs);
    }


    public static MethodNode getMethod(ClassNode node, String name, String desc) {
        if (node.methods == null) return null;
        for (var method : node.methods) {
            if (name != null && !name.equals(method.name)) continue;
            if (desc != null && !desc.equals(method.desc)) continue;

            return method;
        }
        return null;
    }

    public static FieldNode getField(ClassNode node, String name, String desc) {
        if (node.methods == null) return null;
        for (var field : node.fields) {
            if (name != null && !name.equals(field.name)) continue;
            if (desc != null && !desc.equals(field.desc)) continue;

            return field;
        }
        return null;
    }

    public static void pushClinit(ClassNode klass, Consumer<MethodVisitor> mv) {
        var clinit = getMethod(klass, "<clinit>", "()V");
        if (clinit == null) {
            var met = klass.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            mv.accept(met);
            met.visitInsn(Opcodes.RETURN);
        } else {
            var met = new MethodNode();
            met.tryCatchBlocks = clinit.tryCatchBlocks;
            met.localVariables = clinit.localVariables;

            mv.accept(met);
            clinit.instructions.insertBefore(
                    clinit.instructions.getFirst(),
                    met.instructions
            );
            clinit.maxLocals = Math.max(clinit.maxLocals, met.maxLocals);
            clinit.maxStack = Math.max(clinit.maxStack, met.maxStack);
        }
    }
}
