package moe.karla.jvmsandbox.transformer.util;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class InvokeDynamicFaker {
    static final Type TYPE_METHOD_HANDLE = Type.getType(MethodHandle.class);
    static final Type TYPE_ATOMIC_REFERENCE = Type.getType(AtomicReference.class);

    static String generateMethodName(
            String methodName, String methodDesc,
            Handle bsm, Object[] bsmArgs
    ) {
        var name = new StringBuilder();
        name.append(methodName);
        name.append("$$invokedynamic$$");
        name.append(Math.abs(methodDesc.hashCode()));
        name.append("$").append(bsm.getName());
        name.append("$").append(Math.abs(bsm.getDesc().hashCode()));
        name.append("$").append(Math.abs(bsm.getOwner().hashCode()));
        if (bsmArgs != null) {
            for (var arg : bsmArgs) {
                name.append('$').append(Math.abs(arg.hashCode()));
            }
        }
        return name.toString();
    }

    static void generate(ClassNode klass, String wrapperMethodName, String methodName, String methodDesc, Handle bsm, Object[] bsmArgs) {
        var isInterface = (klass.access & Opcodes.ACC_INTERFACE) != 0;

        if (isInterface) {
            klass.visitField(
                    Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL,
                    wrapperMethodName,
                    TYPE_ATOMIC_REFERENCE.getDescriptor(),
                    null,
                    null
            );
            ASMUtil.pushClinit(klass, mv -> {
                mv.visitTypeInsn(Opcodes.NEW, TYPE_ATOMIC_REFERENCE.getInternalName());
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, TYPE_ATOMIC_REFERENCE.getInternalName(), "<init>", "()V", false);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, klass.name, wrapperMethodName, TYPE_ATOMIC_REFERENCE.getDescriptor());
            });
        } else {
            klass.visitField(
                    Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
                    wrapperMethodName,
                    TYPE_METHOD_HANDLE.getDescriptor(),
                    null,
                    null
            );
        }

        var mv = klass.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                wrapperMethodName, methodDesc,
                null, null
        );
        if (isInterface) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, klass.name, wrapperMethodName, TYPE_ATOMIC_REFERENCE.getDescriptor());
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TYPE_ATOMIC_REFERENCE.getInternalName(), "get", "()Ljava/lang/Object;", false);
            mv.visitTypeInsn(Opcodes.CHECKCAST, TYPE_METHOD_HANDLE.getInternalName());
        } else {
            mv.visitFieldInsn(Opcodes.GETSTATIC, klass.name, wrapperMethodName, TYPE_METHOD_HANDLE.getDescriptor());
        }
        mv.visitInsn(Opcodes.DUP);
        var realInvoke = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, realInvoke);
        mv.visitInsn(Opcodes.POP);

        // region invoke metafactory

        ASMUtil.getLookup(mv);
        mv.visitLdcInsn(Type.getObjectType(bsm.getOwner()));
        mv.visitLdcInsn(bsm.getName());
        ASMUtil.emitMethodType(mv, klass.name, bsm.getDesc());
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);


        var argsLength = bsmArgs == null ? 0 : bsmArgs.length;
        var argsCount = Type.getArgumentCount(bsm.getDesc());
        var bootstrapDescType = Type.getMethodType(bsm.getDesc());
        var bootstrapLastArg = bootstrapDescType.getArgumentTypes()[bootstrapDescType.getArgumentCount() - 1];


        if (bootstrapLastArg.getSort() == Type.ARRAY) { // vararg
            mv.visitLdcInsn(Type.getType(Object[].class));
            mv.visitLdcInsn(argsLength + 3 - (argsCount - 1));

            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asCollector", "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;", false);
        }
        mv.visitLdcInsn(Type.getType(Object[].class));
        mv.visitLdcInsn(argsLength + 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asSpreader", "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitLdcInsn(Type.getType(Object[].class));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asVarargsCollector", "(Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false);


        ArrayList<Type> descTypes = new ArrayList<>(List.of(
                Type.getType(Object.class),
                Type.getType(Object.class),
                Type.getType(Object.class)
        ));
        ASMUtil.getLookup(mv);
        mv.visitLdcInsn(methodName);
        ASMUtil.emitMethodType(mv, klass.name, methodDesc);
        if (bsmArgs != null) {
            for (var arg : bsmArgs) {
                mv.visitLdcInsn(arg);

                if (arg instanceof Long) {
                    descTypes.add(Type.LONG_TYPE);
                } else if (arg instanceof Double) {
                    descTypes.add(Type.DOUBLE_TYPE);
                } else if (arg instanceof Float) {
                    descTypes.add(Type.FLOAT_TYPE);
                } else if (arg instanceof Number) {
                    descTypes.add(Type.INT_TYPE);
                } else {
                    descTypes.add(Type.getType(Object.class));
                }
            }
        }

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", Type.getMethodDescriptor(
                Type.getType(Object.class),
                descTypes.toArray(new Type[0])
        ), false);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/invoke/CallSite");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/CallSite", "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;", false);

        mv.visitInsn(Opcodes.DUP);

        if (isInterface) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, klass.name, wrapperMethodName, TYPE_ATOMIC_REFERENCE.getDescriptor());
            mv.visitInsn(Opcodes.SWAP);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TYPE_ATOMIC_REFERENCE.getInternalName(), "set", "(Ljava/lang/Object;)V", false);
        } else {
            mv.visitFieldInsn(Opcodes.PUTSTATIC, klass.name, wrapperMethodName, TYPE_METHOD_HANDLE.getDescriptor());
        }

        // endregion

        mv.visitLabel(realInvoke);
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/invoke/MethodHandle"});


        var methodDescType = Type.getMethodType(methodDesc);
        ASMUtil.pushLocalsToStack(mv, 0, methodDescType.getArgumentTypes());
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", methodDesc, false);
        ASMUtil.popStackReturn(mv, methodDescType.getReturnType());

    }

    static AbstractInsnNode pushInvokeDynamic(
            ClassNode klass,
            String methodName, String methodDesc,
            Handle bsm, Object[] bsmArgs
    ) {
        var wrapperMethodName = generateMethodName(methodName, methodDesc, bsm, bsmArgs);
        if (ASMUtil.getMethod(klass, wrapperMethodName, methodDesc) == null) {
            generate(klass, wrapperMethodName, methodName, methodDesc, bsm, bsmArgs);
        }

        return new MethodInsnNode(
                Opcodes.INVOKESTATIC, klass.name,
                wrapperMethodName, methodDesc,
                (klass.access & Opcodes.ACC_INTERFACE) != 0
        );
    }
}
