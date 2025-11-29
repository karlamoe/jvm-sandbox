package moe.karla.jvmsandbox.instructment.generator;

import moe.karla.jvmsandbox.runtime.util.InvokeHelper;
import moe.karla.jvmsandbox.transformer.context.MethodTransformContext;
import moe.karla.jvmsandbox.transformer.context.TransformContext;
import moe.karla.jvmsandbox.transformer.interpreter.TransformInterpreter;
import moe.karla.jvmsandbox.transformer.util.ASMUtil;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
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

        iterator.set(ASMUtil.pushInvokeDynamic(
                klass, noInvokeDynamic,
                methodName, methodDesc,
                new Handle(Opcodes.H_INVOKESTATIC, targetClass, bootstrapName, bootstrapDesc, false),
                args
        ));
    }

    @Override
    public void interpretObjectNew(MethodTransformContext context, ListIterator<AbstractInsnNode> iterator, MethodInsnNode node) {
        replaceAsTargetInvokeDynamic(
                context.klass, context.method, context, iterator,
                "_", node.desc, NAME_HOOK_NORMAL, DESC_HOOK_NORMAL,
                Type.getObjectType(node.owner),
                MethodHandleInfo.REF_newInvokeSpecial
        );
    }

    @SuppressWarnings("RedundantIfStatement")
    protected boolean shouldSkipInterpretMethodCall(MethodTransformContext context, ListIterator<AbstractInsnNode> iterator, MethodInsnNode node) {
        if ("java/lang/invoke/MethodHandle".equals(node.owner)) {
            // meaningless
            return node.name.equals("invoke") || node.name.equals("invokeExact");
        }

        if (context.klass.name.equals(node.owner) && ASMUtil.getMethod(context.klass, node.name, node.desc) != null) {
            // self reference
            return true;
        }

        if (node.owner.charAt(0) == '[') {
            // Array operations: Object[].clone()
            return true;
        }

        return false;
    }

    @Override
    public void interpretMethodCall(MethodTransformContext context, ListIterator<AbstractInsnNode> iterator, MethodInsnNode node) {
        if (shouldSkipInterpretMethodCall(context, iterator, node)) {
            return;
        }

        if (node.getOpcode() == Opcodes.INVOKESTATIC) {
            replaceAsTargetInvokeDynamic(
                    context.klass, context.method, context, iterator,
                    node.name, node.desc, NAME_HOOK_NORMAL, DESC_HOOK_NORMAL,
                    Type.getObjectType(node.owner),
                    MethodHandleInfo.REF_invokeStatic
            );
        } else {
            replaceAsTargetInvokeDynamic(
                    context.klass, context.method, context, iterator,
                    node.name,
                    "(" + Type.getObjectType(node.owner).getDescriptor() + node.desc.substring(1),
                    NAME_HOOK_NORMAL,
                    DESC_HOOK_NORMAL,
                    Type.getObjectType(node.owner),
                    switch (node.getOpcode()) {
                        case Opcodes.INVOKEVIRTUAL -> MethodHandleInfo.REF_invokeVirtual;
                        case Opcodes.INVOKESPECIAL -> MethodHandleInfo.REF_invokeSpecial;
                        case Opcodes.INVOKEINTERFACE -> MethodHandleInfo.REF_invokeInterface;
                        default -> throw new AssertionError("Unknown opcode " + node.getOpcode());
                    }
            );
        }
    }

    @Override
    public void interpretFieldCall(MethodTransformContext context, ListIterator<AbstractInsnNode> iterator, FieldInsnNode node) {
        if (node.owner.equals(context.klass.name) && ASMUtil.getField(context.klass, node.name, node.desc) != null) {
            // self reference
            return;
        }

        replaceAsTargetInvokeDynamic(
                context.klass, context.method, context, iterator,
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
    public void interpretSuperConstructorCall(MethodTransformContext context, ListIterator<AbstractInsnNode> iterator, MethodInsnNode node) throws Throwable {
        super.interpretSuperConstructorCall(context, iterator, node);
        iterator.previous();

        var method = context.method;
        var klass = context.klass;

        var frames = new Analyzer<>(new BasicInterpreter()).analyze(klass.name, method);
        var index = method.instructions.indexOf(node);
        var frame = frames[index];

        var localStart = 2;
        for (var i = 0; i < frame.getLocals(); i++) {
            localStart += frame.getLocal(i).getSize();
        }

        // step1: save stack to locals
        var maxStack = localStart;
        for (int i = frame.getStackSize() - 1; i > 0; i--) {
            var stack = frame.getStack(i);
            iterator.add(new VarInsnNode(stack.getType().getOpcode(Opcodes.ISTORE), maxStack));
            maxStack += stack.getType().getSize();
        }


        // step2: push locals to stack again
        for (int tmpStack = maxStack, i = 1; i < frame.getStackSize(); i++) {
            var stack = frame.getStack(i);
            tmpStack -= stack.getType().getSize();
            iterator.add(new VarInsnNode(stack.getType().getOpcode(Opcodes.ILOAD), tmpStack));
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
        for (int tmpStack = maxStack, i = 1; i < frame.getStackSize(); i++) {
            var stack = frame.getStack(i);
            tmpStack -= stack.getType().getSize();
            iterator.add(new VarInsnNode(stack.getType().getOpcode(Opcodes.ILOAD), tmpStack));
        }

        iterator.next();
    }


    protected List<Object> packBootstrapWithArguments(
            MethodTransformContext context,
            Handle bsm, Object[] bsmArgs
    ) {

        List<Object> args = new ArrayList<>();
        args.add(Type.getObjectType(bsm.getOwner()));
        args.add(bsm.getName());
        args.add(Type.getMethodType(bsm.getDesc()));
        if (bsmArgs != null) {
            for (var arg : bsmArgs) {
                args.add(interpretLdcValue(context, arg));
            }
        }

        return args;
    }

    @Override
    public void interpretDynamicCall(MethodTransformContext context, ListIterator<AbstractInsnNode> iterator, InvokeDynamicInsnNode node) {
        replaceAsTargetInvokeDynamic(
                context.klass, context.method, context, iterator,
                node.name,
                node.desc,
                NAME_HOOK_DYNAMIC,
                DESC_HOOK_DYNAMIC,
                packBootstrapWithArguments(
                        context,
                        node.bsm, node.bsmArgs
                ).toArray()
        );
    }

    @Override
    public void interpretLdcInsn(MethodTransformContext context, ListIterator<AbstractInsnNode> iterator, LdcInsnNode node) {
        node.cst = interpretLdcValue(context, node.cst);
    }

    public Object interpretLdcValue(
            MethodTransformContext context,
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
                            context,
                            dynamic.getBootstrapMethod(),
                            (Object[]) interpretLdcValue(
                                    context,
                                    ASMUtil.getBSMArguments(dynamic)
                            )
                    ).toArray()
            );
        } else if (value instanceof Object[] array) {
            for (var i = 0; i < array.length; i++) {
                array[i] = interpretLdcValue(context, array[i]);
            }
        }
        return value;
    }
}
