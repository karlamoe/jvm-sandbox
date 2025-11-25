package moe.karla.jvmsandbox.transformer.util;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ASMUtil {
    public static void generateConstructor(ClassVisitor visitor, String superKlass) {
        MethodVisitor methodVisitor = visitor.visitMethod(
                Opcodes.ACC_PRIVATE,
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
}
