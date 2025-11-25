package moe.karla.jvmsandbox.instructment.generator;

import moe.karla.jvmsandbox.transformer.TransformContext;
import moe.karla.jvmsandbox.transformer.interpreter.TransformInterpreter;
import moe.karla.jvmsandbox.transformer.util.ASMUtil;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

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

    public HookWrapperInterpreter(String targetClass) {
        this.targetClass = targetClass;
    }

    private void insertHostInvokeDynamic(
            ClassNode klass, MethodNode method, TransformContext context,
            ListIterator<AbstractInsnNode> iterator,
            String methodName, String methodDesc,
            String bootstrapName, String bootstrapDesc,
            Object... args
    ) {
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
        insertHostInvokeDynamic(
                klass, method, context, iterator,
                "_", node.desc, NAME_HOOK_NORMAL, DESC_HOOK_NORMAL,
                Type.getObjectType(node.owner),
                MethodHandleInfo.REF_newInvokeSpecial
        );
    }

    @Override
    public void interpretMethodCall(ClassNode klass, MethodNode method, TransformContext context, ListIterator<AbstractInsnNode> iterator, MethodInsnNode node) {
        if (node.getOpcode() == Opcodes.INVOKESTATIC) {
            insertHostInvokeDynamic(
                    klass, method, context, iterator,
                    node.name, node.desc, NAME_HOOK_NORMAL, DESC_HOOK_NORMAL,
                    Type.getObjectType(node.owner),
                    MethodHandleInfo.REF_invokeStatic
            );
        } else {
            insertHostInvokeDynamic(
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
        insertHostInvokeDynamic(
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
        insertHostInvokeDynamic(
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
