package moe.karla.jvmsandbox.transformer.util;

import org.objectweb.asm.*;

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
}
