package moe.karla.jvmsandbox.instructment.generator;

import moe.karla.jvmsandbox.runtime.SandboxInitializationHolder;
import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static moe.karla.jvmsandbox.transformer.util.ASMUtil.generateConstructor;

public class HookWrapperGenerator {
    private static final Type TYPE_SANDBOX_RUNTIME = Type.getType(SandboxRuntime.class);
    public static final ThreadLocal<SandboxRuntime> PROVIDING_RUNTIME = SandboxInitializationHolder.PROVIDING_RUNTIME;

    public static void generate(ClassVisitor visitor, String klassName) {
        visitor.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                klassName,
                null,
                "java/lang/Object",
                null
        );
        generateConstructor(visitor, "java/lang/Object");

        visitor.visitField(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC,
                "runtime",
                TYPE_SANDBOX_RUNTIME.getDescriptor(),
                null, null);

        {
            var clinit = visitor.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);

            clinit.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(SandboxInitializationHolder.class), "PROVIDING_RUNTIME", Type.getDescriptor(ThreadLocal.class));
            clinit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ThreadLocal.class), "get", "()Ljava/lang/Object;", false);
            clinit.visitTypeInsn(Opcodes.CHECKCAST, TYPE_SANDBOX_RUNTIME.getInternalName());

            clinit.visitFieldInsn(Opcodes.PUTSTATIC, klassName, "runtime", TYPE_SANDBOX_RUNTIME.getDescriptor());
            clinit.visitInsn(Opcodes.RETURN);
            clinit.visitMaxs(2, 0);
            clinit.visitEnd();
        }

        {
            var mv = visitor.visitMethod(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    HookWrapperInterpreter.NAME_HOOK_NORMAL,
                    HookWrapperInterpreter.DESC_HOOK_NORMAL,
                    null, null
            );

            mv.visitFieldInsn(Opcodes.GETSTATIC, klassName, "runtime", TYPE_SANDBOX_RUNTIME.getDescriptor());
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ILOAD, 4);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    TYPE_SANDBOX_RUNTIME.getInternalName(),
                    "interpretInvoke",
                    MethodType.methodType(
                            CallSite.class,
                            MethodHandles.Lookup.class,
                            Class.class,
                            String.class,
                            MethodType.class,
                            int.class
                    ).toMethodDescriptorString(),
                    false
            );
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(6, 5);
            mv.visitEnd();
        }
        {
            var mv = visitor.visitMethod(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VARARGS,
                    HookWrapperInterpreter.NAME_HOOK_DYNAMIC,
                    HookWrapperInterpreter.DESC_HOOK_DYNAMIC,
                    null, null
            );

            mv.visitFieldInsn(Opcodes.GETSTATIC, klassName, "runtime", TYPE_SANDBOX_RUNTIME.getDescriptor());
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            mv.visitVarInsn(Opcodes.ALOAD, 5);
            mv.visitVarInsn(Opcodes.ALOAD, 6);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    TYPE_SANDBOX_RUNTIME.getInternalName(),
                    "interpretInvokeDynamic",
                    MethodType.methodType(
                            CallSite.class,
                            MethodHandles.Lookup.class,
                            String.class,
                            MethodType.class,
                            Class.class,
                            String.class,
                            MethodType.class,
                            Object[].class
                    ).toMethodDescriptorString(),
                    false
            );
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(8, 7);
            mv.visitEnd();
        }

        visitor.visitEnd();
    }
}
