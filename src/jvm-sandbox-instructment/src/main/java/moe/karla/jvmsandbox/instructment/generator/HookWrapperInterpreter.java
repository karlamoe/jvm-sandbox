package moe.karla.jvmsandbox.instructment.generator;

import moe.karla.jvmsandbox.runtime.util.InvokeHelper;
import moe.karla.jvmsandbox.transformer.TransformContext;
import moe.karla.jvmsandbox.transformer.interpreter.TransformInterpreter;
import moe.karla.jvmsandbox.transformer.util.ASMUtil;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicLong;

public class HookWrapperInterpreter extends TransformInterpreter {
    public static final String DESC_HOOK_NORMAL = MethodType.methodType(
            CallSite.class,
            MethodHandles.Lookup.class,  // caller
            String.class,             // method name
            MethodType.class,                    // method desc
            Class.class,                         // owner
            int.class                            // refType
    ).toMethodDescriptorString();
    public static final String DESC_HOOK_DYNAMIC = MethodType.methodType(
            CallSite.class,
            MethodHandles.Lookup.class,  // caller
            String.class,             // dynamic name
            MethodType.class,                    // dynamic desc
            Class.class,                         // metafactory owner
            String.class,                        // metafactory method name
            MethodType.class,                    // metafactory method desc
            Object[].class                       // metafactory method args
    ).toMethodDescriptorString();
    public static final String DESC_HOOK_DYNAMIC_CONSTANT = MethodType.methodType(
            Object.class,
            MethodHandles.Lookup.class,  // caller
            String.class,             // dynamic name
            Class.class,                        // dynamic type
            Class.class,                         // metafactory owner
            String.class,                        // metafactory method name
            MethodType.class,                    // metafactory method desc
            Object[].class                       // metafactory method args
    ).toMethodDescriptorString();
    public static final String NAME_HOOK_NORMAL = "hookNormal";
    public static final String NAME_HOOK_DYNAMIC = "hookDynamic";
    public static final String NAME_HOOK_DYNAMIC_CONSTANT = "hookDynamicConstant";

    private final String targetClass;
    private final boolean noInvokeDynamic;
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis() ^ System.nanoTime());

    public HookWrapperInterpreter(String targetClass) {
        this(targetClass, false);
    }

    public HookWrapperInterpreter(String targetClass, boolean noInvokeDynamic) {
        this.targetClass = targetClass;
        this.noInvokeDynamic = noInvokeDynamic;
    }

    private void replaceAsTargetInvokeDynamic(
            ClassNode klass, MethodNode method, TransformContext context,
            ListIterator<AbstractInsnNode> iterator,
            String methodName, String methodDesc,
            String bootstrapName, String bootstrapDesc,
            Object... args
    ) {
        if (noInvokeDynamic || (klass.version < Opcodes.V1_8)) {
            var descMH = "Ljava/lang/invoke/MethodHandle;";

            var wrapperMethodName = ("$$$invokedynamic$$$destructed$$$" + System.currentTimeMillis() + "$" + sequence.incrementAndGet()).replace('-', '_');
            klass.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, wrapperMethodName, descMH, null, null);

            var forwardedMethod = klass.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, wrapperMethodName, methodDesc, null, null);
            iterator.set(new MethodInsnNode(Opcodes.INVOKESTATIC, klass.name, wrapperMethodName, methodDesc));

            forwardedMethod.visitFieldInsn(Opcodes.GETSTATIC, klass.name, wrapperMethodName, descMH);
            forwardedMethod.visitInsn(Opcodes.DUP);
            var realInvoke = new Label();
            forwardedMethod.visitJumpInsn(Opcodes.IFNONNULL, realInvoke);
            forwardedMethod.visitInsn(Opcodes.POP);

            // region invoke metafactory

            ASMUtil.getLookup(forwardedMethod);
            forwardedMethod.visitLdcInsn(Type.getObjectType(targetClass));
            forwardedMethod.visitLdcInsn(bootstrapName);
            ASMUtil.emitMethodType(forwardedMethod, klass.name, bootstrapDesc);
            forwardedMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);


            var argsLength = args == null ? 0 : args.length;
            var argsCount = Type.getArgumentCount(bootstrapDesc);
            var bootstrapDescType = Type.getMethodType(bootstrapDesc);
            var bootstrapLastArg = bootstrapDescType.getArgumentTypes()[bootstrapDescType.getArgumentCount() - 1];


            if (bootstrapLastArg.getSort() == Type.ARRAY) { // vararg
                forwardedMethod.visitLdcInsn(Type.getType(Object[].class));
                forwardedMethod.visitLdcInsn(argsLength + 3 - (argsCount - 1));

                forwardedMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asCollector", "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;", false);
            }
            forwardedMethod.visitLdcInsn(Type.getType(Object[].class));
            forwardedMethod.visitLdcInsn(argsLength + 3);
            forwardedMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asSpreader", "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;", false);
            forwardedMethod.visitLdcInsn(Type.getType(Object[].class));
            forwardedMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asVarargsCollector", "(Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false);


            ArrayList<Type> descTypes = new ArrayList<>(List.of(
                    Type.getType(Object.class),
                    Type.getType(Object.class),
                    Type.getType(Object.class)
            ));
            ASMUtil.getLookup(forwardedMethod);
            forwardedMethod.visitLdcInsn(methodName);
            ASMUtil.emitMethodType(forwardedMethod, klass.name, methodDesc);
            if (args != null) {
                for (var arg : args) {
                    forwardedMethod.visitLdcInsn(arg);

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

            forwardedMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", Type.getMethodDescriptor(
                    Type.getType(Object.class),
                    descTypes.toArray(new Type[0])
            ), false);
            forwardedMethod.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/invoke/CallSite");
            forwardedMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/CallSite", "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;", false);

            forwardedMethod.visitInsn(Opcodes.DUP);
            forwardedMethod.visitFieldInsn(Opcodes.PUTSTATIC, klass.name, wrapperMethodName, descMH);

            // endregion

            forwardedMethod.visitLabel(realInvoke);
            forwardedMethod.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/invoke/MethodHandle"});


            var methodDescType = Type.getMethodType(methodDesc);
            ASMUtil.pushLocalsToStack(forwardedMethod, 0, methodDescType.getArgumentTypes());
            forwardedMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", methodDesc, false);
            ASMUtil.popStackReturn(forwardedMethod, methodDescType.getReturnType());

            return;
        }

        iterator.set(
                new InvokeDynamicInsnNode(
                        methodName, methodDesc,
                        new Handle(Opcodes.H_INVOKESTATIC, targetClass, bootstrapName, bootstrapDesc, false),
                        args
                )
        );
    }

    @Override
    public void interpretObjectNew(ClassNode klass, MethodNode method, TransformContext context, ListIterator<AbstractInsnNode> iterator, MethodInsnNode node) {
        replaceAsTargetInvokeDynamic(
                klass, method, context, iterator,
                "_", node.desc, NAME_HOOK_NORMAL, DESC_HOOK_NORMAL,
                Type.getObjectType(node.owner),
                MethodHandleInfo.REF_newInvokeSpecial
        );
    }

    protected boolean shouldSkipInterpretMethodCall(ClassNode klass, MethodNode method, TransformContext context, ListIterator<AbstractInsnNode> iterator, MethodInsnNode node) {
        if ("java/lang/invoke/MethodHandle".equals(node.owner)) {
            // meaningless
            return node.name.equals("invoke") || node.name.equals("invokeExact");
        }

        return false;
    }

    @Override
    public void interpretMethodCall(ClassNode klass, MethodNode method, TransformContext context, ListIterator<AbstractInsnNode> iterator, MethodInsnNode node) {
        if (shouldSkipInterpretMethodCall(klass, method, context, iterator, node)) {
            return;
        }

        if (node.getOpcode() == Opcodes.INVOKESTATIC) {
            replaceAsTargetInvokeDynamic(
                    klass, method, context, iterator,
                    node.name, node.desc, NAME_HOOK_NORMAL, DESC_HOOK_NORMAL,
                    Type.getObjectType(node.owner),
                    MethodHandleInfo.REF_invokeStatic
            );
        } else {
            replaceAsTargetInvokeDynamic(
                    klass, method, context, iterator,
                    node.name,
                    "(L" + node.owner + ";" + node.desc.substring(1),
                    NAME_HOOK_NORMAL,
                    DESC_HOOK_NORMAL,
                    Type.getObjectType(node.owner),
                    switch (node.getOpcode()) {
                        case Opcodes.INVOKEVIRTUAL -> MethodHandleInfo.REF_invokeVirtual;
                        case Opcodes.INVOKESPECIAL -> MethodHandleInfo.REF_invokeSpecial;
                        case Opcodes.INVOKEINTERFACE -> MethodHandleInfo.REF_invokeInterface;
                        default -> node.getOpcode();
                    }
            );
        }
    }

    @Override
    public void interpretFieldCall(ClassNode klass, MethodNode method, TransformContext context, ListIterator<AbstractInsnNode> iterator, FieldInsnNode node) {
        replaceAsTargetInvokeDynamic(
                klass, method, context, iterator,
                node.name,
                switch (node.getOpcode()) {
                    case Opcodes.GETFIELD -> "(L" + node.owner + ";)" + node.desc;
                    case Opcodes.PUTFIELD -> "(L" + node.owner + ";" + node.desc + ")V";
                    case Opcodes.GETSTATIC -> "()" + node.desc;
                    case Opcodes.PUTSTATIC -> "(" + node.desc + ")V";
                    default -> throw new AssertionError();
                },
                NAME_HOOK_NORMAL,
                DESC_HOOK_NORMAL,
                Type.getObjectType(node.owner),
                switch (node.getOpcode()) {
                    case Opcodes.GETFIELD -> MethodHandleInfo.REF_getField;
                    case Opcodes.PUTFIELD -> MethodHandleInfo.REF_putField;
                    case Opcodes.GETSTATIC -> MethodHandleInfo.REF_getStatic;
                    case Opcodes.PUTSTATIC -> MethodHandleInfo.REF_putStatic;
                    default -> node.getOpcode();
                }
        );
    }

    @Override
    public void interpretSuperConstructorCall(ClassNode klass, MethodNode method, TransformContext context, ListIterator<AbstractInsnNode> iterator, MethodInsnNode node) throws Throwable {
        super.interpretSuperConstructorCall(klass, method, context, iterator, node);
        iterator.previous();

        var frames = new Analyzer<>(new BasicInterpreter()).analyze(klass.name, method);
        var index = method.instructions.indexOf(node);
        var frame = frames[index];

        var localStart = 2;
        for (var i = 0; i < frame.getLocals(); i++) {
            localStart += frame.getLocal(i).getSize();
        }

        // step1: save stack to locals
        for (int tmpStack = localStart, i = frame.getStackSize() - 1; i > 0; i--) {
            var stack = frame.getStack(i);
            iterator.add(new VarInsnNode(stack.getType().getOpcode(Opcodes.ISTORE), tmpStack));
            tmpStack += stack.getType().getSize();
        }

        // step2: push locals to stack again
        for (int tmpStack = localStart, i = 1; i < frame.getStackSize(); i++) {
            var stack = frame.getStack(i);
            iterator.add(new VarInsnNode(stack.getType().getOpcode(Opcodes.ILOAD), tmpStack));
            tmpStack += stack.getType().getSize();
        }

        // step3: call before construct check
        iterator.add(new InsnNode(Opcodes.NOP));
        iterator.previous();
        iterator.next();
        replaceAsTargetInvokeDynamic(
                klass, method, context, iterator,
                "_", node.desc, NAME_HOOK_NORMAL, DESC_HOOK_NORMAL,
                Type.getObjectType(klass.superName),
                InvokeHelper.EXREF_beforeConstructor
        );

        // step4: recover stacks
        for (int tmpStack = localStart, i = 1; i < frame.getStackSize(); i++) {
            var stack = frame.getStack(i);
            iterator.add(new VarInsnNode(stack.getType().getOpcode(Opcodes.ILOAD), tmpStack));
            tmpStack += stack.getType().getSize();
        }

        iterator.next();
    }


    protected List<Object> packBootstrapWithArguments(
            ClassNode klass, MethodNode method, TransformContext context,
            Handle bsm, Object[] bsmArgs
    ) {

        List<Object> args = new ArrayList<>();
        args.add(Type.getObjectType(bsm.getOwner()));
        args.add(bsm.getName());
        args.add(Type.getMethodType(bsm.getDesc()));
        if (bsmArgs != null) {
            for (var arg : bsmArgs) {
                args.add(interpretLdcValue(klass, method, context, arg));
            }
        }

        return args;
    }

    @Override
    public void interpretDynamicCall(ClassNode klass, MethodNode method, TransformContext context, ListIterator<AbstractInsnNode> iterator, InvokeDynamicInsnNode node) {
        replaceAsTargetInvokeDynamic(
                klass, method, context, iterator,
                node.name,
                node.desc,
                NAME_HOOK_DYNAMIC,
                DESC_HOOK_DYNAMIC,
                packBootstrapWithArguments(
                        klass, method, context,
                        node.bsm, node.bsmArgs
                ).toArray()
        );
    }

    @Override
    public void interpretLdcInsn(ClassNode klass, MethodNode method, TransformContext context, ListIterator<AbstractInsnNode> iterator, LdcInsnNode node) {
        node.cst = interpretLdcValue(klass, method, context, node.cst);
    }

    public Object interpretLdcValue(
            ClassNode klass, MethodNode method, TransformContext context,
            Object value
    ) {
        if (value instanceof ConstantDynamic dynamic) {
            return new ConstantDynamic(
                    dynamic.getName(),
                    dynamic.getDescriptor(),
                    new Handle(Opcodes.H_INVOKESTATIC, targetClass,
                            NAME_HOOK_DYNAMIC_CONSTANT, DESC_HOOK_DYNAMIC_CONSTANT,
                            false),
                    packBootstrapWithArguments(
                            klass, method, context,
                            dynamic.getBootstrapMethod(),
                            (Object[]) interpretLdcValue(
                                    klass, method, context,
                                    ASMUtil.getBSMArguments(dynamic)
                            )
                    ).toArray()
            );
        } else if (value instanceof Object[] array) {
            for (var i = 0; i < array.length; i++) {
                array[i] = interpretLdcValue(klass, method, context, array[i]);
            }
        }
        return value;
    }
}
