package moe.karla.jvmsandbox.transformer.transformers;

import moe.karla.jvmsandbox.transformer.context.ApplicationTransformContext;
import moe.karla.jvmsandbox.transformer.Transformer;
import moe.karla.jvmsandbox.transformer.util.ASMUtil;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LambdaDeoptimizeTransformer extends Transformer {
    @Override
    public ClassNode transform(ClassNode node, ApplicationTransformContext context) throws Throwable {
        var prefix = "$$redirect$$" + node.name.substring(node.name.lastIndexOf('/') + 1) + "$$access$$deoptimize$$";
        var trans = new Object() {
            private final Map<String, Integer> counters = new HashMap<>();

            public Object proc(Object cst) {
                if (cst instanceof ConstantDynamic dynamic) {
                    var newBsmArgs = ASMUtil.getBSMArguments(dynamic);
                    proc(newBsmArgs);
                    return new ConstantDynamic(
                            dynamic.getName(),
                            dynamic.getDescriptor(),
                            dynamic.getBootstrapMethod(),
                            newBsmArgs
                    );
                } else if (cst instanceof Handle handle) {
                    // self reference.
                    if (handle.getOwner().equals(node.name)) return handle;

                    var hash = handle.getTag() + ',' + handle.getOwner() + '.' + handle.getName() + handle.getDesc();

                    var paddedMethodDesc = switch (handle.getTag()) {
                        // fields
                        case Opcodes.H_GETSTATIC -> "()" + handle.getDesc();
                        case Opcodes.H_PUTSTATIC -> "(" + handle.getDesc() + ")V";
                        case Opcodes.H_GETFIELD -> "(L" + handle.getOwner() + ";)" + handle.getDesc();
                        case Opcodes.H_PUTFIELD -> "(L" + handle.getOwner() + ";" + handle.getDesc() + ")V";

                        // methods
                        case Opcodes.H_INVOKESTATIC -> handle.getDesc();
                        case Opcodes.H_NEWINVOKESPECIAL ->
                                handle.getDesc().substring(0, handle.getDesc().lastIndexOf(')')) + ")L" + handle.getOwner() + ";";

                        case Opcodes.H_INVOKEVIRTUAL,
                             Opcodes.H_INVOKEINTERFACE,
                             Opcodes.H_INVOKESPECIAL -> "(L" + handle.getOwner() + ";" + handle.getDesc().substring(1);


                        default -> throw new AssertionError();
                    };

                    var accCount = counters.get(hash);
                    if (accCount == null) {
                        accCount = counters.size();
                        counters.put(hash, accCount);

                        var mv = node.visitMethod(
                                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                                prefix + accCount, paddedMethodDesc, null, null
                        );
                        switch (handle.getTag()) {
                            // fields
                            case Opcodes.H_GETSTATIC -> {
                                mv.visitFieldInsn(Opcodes.GETSTATIC, handle.getOwner(), handle.getName(), handle.getDesc());
                                mv.visitInsn(Type.getType(handle.getDesc()).getOpcode(Opcodes.IRETURN));
                            }
                            case Opcodes.H_PUTSTATIC -> {
                                mv.visitVarInsn(Type.getType(handle.getDesc()).getOpcode(Opcodes.ILOAD), 0);
                                mv.visitFieldInsn(Opcodes.PUTSTATIC, handle.getOwner(), handle.getName(), handle.getDesc());
                                mv.visitInsn(Opcodes.RETURN);
                            }
                            case Opcodes.H_GETFIELD -> {
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                mv.visitFieldInsn(Opcodes.GETFIELD, handle.getOwner(), handle.getName(), handle.getDesc());
                                mv.visitInsn(Type.getType(handle.getDesc()).getOpcode(Opcodes.IRETURN));
                            }
                            case Opcodes.H_PUTFIELD -> {
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                mv.visitVarInsn(Type.getType(handle.getDesc()).getOpcode(Opcodes.ILOAD), 1);
                                mv.visitFieldInsn(Opcodes.PUTFIELD, handle.getOwner(), handle.getName(), handle.getDesc());
                                mv.visitInsn(Opcodes.RETURN);
                            }

                            // methods
                            case Opcodes.H_INVOKESTATIC -> {
                                ASMUtil.pushLocalsToStack(mv, 0, Type.getArgumentTypes(handle.getDesc()));
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, handle.getOwner(), handle.getName(), handle.getDesc(), handle.isInterface());
                                ASMUtil.popStackReturn(mv, Type.getReturnType(handle.getDesc()));
                            }
                            case Opcodes.H_NEWINVOKESPECIAL -> {
                                mv.visitTypeInsn(Opcodes.NEW, handle.getOwner());
                                mv.visitInsn(Opcodes.DUP);

                                ASMUtil.pushLocalsToStack(mv, 0, Type.getArgumentTypes(handle.getDesc()));
                                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, handle.getOwner(), "<init>", handle.getDesc(), handle.isInterface());

                                mv.visitInsn(Opcodes.ARETURN);
                            }

                            case Opcodes.H_INVOKEVIRTUAL -> {
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                ASMUtil.pushLocalsToStack(mv, 1, Type.getArgumentTypes(handle.getDesc()));
                                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, handle.getOwner(), handle.getName(), handle.getDesc(), handle.isInterface());
                                ASMUtil.popStackReturn(mv, Type.getReturnType(handle.getDesc()));
                            }
                            case Opcodes.H_INVOKEINTERFACE -> {
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                ASMUtil.pushLocalsToStack(mv, 1, Type.getArgumentTypes(handle.getDesc()));
                                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, handle.getOwner(), handle.getName(), handle.getDesc(), handle.isInterface());
                                ASMUtil.popStackReturn(mv, Type.getReturnType(handle.getDesc()));
                            }
                            case Opcodes.H_INVOKESPECIAL -> {
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                ASMUtil.pushLocalsToStack(mv, 1, Type.getArgumentTypes(handle.getDesc()));
                                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, handle.getOwner(), handle.getName(), handle.getDesc(), handle.isInterface());
                                ASMUtil.popStackReturn(mv, Type.getReturnType(handle.getDesc()));
                            }


                            default -> throw new AssertionError();
                        }
                    }


                    return new Handle(
                            Opcodes.H_INVOKESTATIC,
                            node.name,
                            prefix + accCount,
                            paddedMethodDesc,
                            (node.access & Opcodes.ACC_INTERFACE) != 0
                    );
                }
                return cst;
            }

            public void proc(Object[] bsmArgs) {
                if (bsmArgs == null) return;
                for (var i = 0; i < bsmArgs.length; i++) {
                    bsmArgs[i] = proc(bsmArgs[i]);
                }
            }
        };
        for (var met : new ArrayList<>(node.methods)) {
            if (met.instructions == null) continue;
            for (var insn : met.instructions) {
                if (insn instanceof InvokeDynamicInsnNode dyn) {
                    trans.proc(dyn.bsmArgs);
                } else if (insn instanceof LdcInsnNode ldc) {
                    ldc.cst = trans.proc(ldc.cst);
                }
            }
        }

        return node;
    }
}
