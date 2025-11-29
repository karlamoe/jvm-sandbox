package moe.karla.jvmsandbox.transformer.transformers;

import moe.karla.jvmsandbox.transformer.context.ApplicationTransformContext;
import moe.karla.jvmsandbox.transformer.Transformer;
import moe.karla.jvmsandbox.transformer.analysis.UninitializedRef;
import moe.karla.jvmsandbox.transformer.analysis.UninitializedThis;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

public class AllocPreProcessTransformer extends Transformer {
    public static final String NAME_CONSTRUCTOR_SUPER_INVOKE = "<constructor_invoke>";
    public static final String NAME_NEW_OBJECT = "<new>";


    @Override
    public ClassNode transform(ClassNode node, ApplicationTransformContext context) throws Throwable {
        class VFrame extends Frame<BasicValue> {
            public VFrame(int numLocals, int maxStack) {
                super(numLocals, maxStack);
            }

            public VFrame(Frame<? extends BasicValue> frame) {
                super(frame);
            }

            @Override
            public void execute(AbstractInsnNode insn, Interpreter<BasicValue> interpreter) throws AnalyzerException {
                if (insn.getOpcode() == Opcodes.INVOKESPECIAL
                        && insn instanceof MethodInsnNode met
                        && met.name.equals("<init>")
                ) {
                    var argCount = Type.getArgumentCount(met.desc);
                    var argThis = getStack(getStackSize() - argCount - 1);

                    if (argThis instanceof UninitializedThis) {
                        // uninitialized this
                        if (!met.owner.equals(node.name)) {
                            // super(); call
                            met.name = NAME_CONSTRUCTOR_SUPER_INVOKE;
                        }
                    } else if (argThis instanceof UninitializedRef) {
                        met.name = NAME_NEW_OBJECT;
                        super.execute(insn, interpreter);
                        var replacement = interpreter.newValue(argThis.getType());
                        for (int i = 0; i < getStackSize(); i++) {
                            var stack = getStack(i);
                            if (stack == argThis) {
                                setStack(i, replacement);
                            }
                        }

                        met.setOpcode(Opcodes.INVOKESTATIC);
                        met.desc = Type.getMethodDescriptor(
                                Type.getObjectType(met.owner),
                                Type.getArgumentTypes(met.desc)
                        );
                        return;
                    } else {
                        throw new AnalyzerException(insn, "Bad <init> this: " + argThis + ", " + argThis.getClass());
                    }
                }
                super.execute(insn, interpreter);
            }
        }

        var analyzer = new Analyzer<>(new BasicInterpreter(Opcodes.ASM9) {
            @Override
            public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
                if (insn.getOpcode() == Opcodes.NEW) {
                    return new UninitializedRef(Type.getObjectType(((TypeInsnNode) insn).desc));
                }
                return super.newOperation(insn);
            }

            @Override
            public BasicValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
                if (isInstanceMethod && local == 0) {
                    return new UninitializedThis(type);
                }
                return super.newParameterValue(isInstanceMethod, local, type);
            }
        }) {
            @Override
            protected Frame<BasicValue> newFrame(Frame<? extends BasicValue> frame) {
                return new VFrame(frame);
            }

            @Override
            protected Frame<BasicValue> newFrame(int numLocals, int numStack) {
                return new VFrame(numLocals, numStack);
            }
        };

        for (var metNode : node.methods) {
            if (metNode.instructions == null) continue;

            var result = analyzer.analyzeAndComputeMaxs(node.name, metNode);

            final var iterator = metNode.instructions.iterator();

            var counter = -1;
            while (iterator.hasNext()) {
                var insn = iterator.next();
                counter++;

                replace:
                {
                    if (insn.getOpcode() == Opcodes.NEW) break replace;
                    if (insn.getOpcode() == Opcodes.DUP
                            || insn.getOpcode() == Opcodes.DUP_X1
                            || insn.getOpcode() == Opcodes.DUP_X2
                    ) {
                        var frame = result[counter];
                        var lastValue = frame.getStack(frame.getStackSize() - 1);
                        if (lastValue instanceof UninitializedRef) {
                            break replace;
                        }
                    }
                    if (insn.getOpcode() == Opcodes.DUP2
                            || insn.getOpcode() == Opcodes.DUP2_X1
                            || insn.getOpcode() == Opcodes.DUP2_X2
                            || insn.getOpcode() == Opcodes.SWAP
                    ) {
                        var frame = result[counter];
                        var lastValue = frame.getStack(frame.getStackSize() - 1);
                        if (lastValue.getSize() != 1) continue;
                        var prevValue = frame.getStack(frame.getStackSize() - 2);

                        var count = 0;
                        if (lastValue instanceof UninitializedRef) count++;
                        if (prevValue instanceof UninitializedRef) count++;

                        if (count == 0) continue;
                        if (count == 1) {
                            if (insn.getOpcode() == Opcodes.SWAP) {
                                break replace;
                            } else if (insn.getOpcode() == Opcodes.DUP2) {
                                // @? -> @?@? -- @@
                                iterator.set(new InsnNode(Opcodes.DUP));
                            } else if (insn.getOpcode() == Opcodes.DUP2_X1) {
                                // a@? -> @?a@? -- @a@ -> DUP_X1
                                iterator.set(new InsnNode(Opcodes.DUP_X1));
                            } else { // DUP2 x2
                                // ab@? -> @?ab@? -- @ab@ -> DUP_X2
                                iterator.set(new InsnNode(Opcodes.DUP_X2));
                            }
                            continue;
                        } else {
                            // count == 2
                            break replace;
                        }
                    }

                    if (insn instanceof MethodInsnNode met && met.name.equals(NAME_NEW_OBJECT)) {
                        var actFrame = new Frame<>(result[counter]);
                        var argCount = Type.getArgumentCount(met.desc);
                        for (var i = 0; i < argCount; i++) {
                            actFrame.pop(); // drop arguments
                        }
                        var argThis = actFrame.pop(); // this

                        var count = 0;
                        for (var i = 0; i < actFrame.getStackSize(); i++) {
                            var stack = actFrame.getStack(i);
                            if (stack == argThis) count++;
                        }

                        if (count == 0) {
                            // directly new() and no object used
                            met.desc = Type.getMethodDescriptor(
                                    Type.VOID_TYPE,
                                    Type.getArgumentTypes(met.desc)
                            );
                            continue;
                        } else if (count == 1 && actFrame.getStack(actFrame.getStackSize() - 1) == argThis) {
                            // normally new() oper
                            continue;
                        } else {
                            // fix stack
                            for (var startIndex = 0; startIndex < actFrame.getStackSize(); startIndex++) {
                                var checkStack = actFrame.getStack(startIndex);

                                if (checkStack == argThis) {
                                    var locals = 0;
                                    for (var i = 0; i < actFrame.getLocals(); i++) {
                                        locals += actFrame.getLocal(i).getSize();
                                    }

                                    var thisSlot = locals;
                                    locals++;
                                    iterator.add(new VarInsnNode(Opcodes.ASTORE, thisSlot));

                                    for (var i = actFrame.getStackSize() - 1; i >= startIndex; i--) {
                                        var stack = actFrame.getStack(i);
                                        if (stack == argThis) continue;
                                        var type = stack.getType();
                                        iterator.add(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), locals));
                                        locals += type.getSize();
                                    }
                                    locals = thisSlot + 1;
                                    for (var i = startIndex; i < actFrame.getStackSize(); i++) {
                                        var stack = actFrame.getStack(i);
                                        if (stack == argThis) {
                                            iterator.add(new VarInsnNode(Opcodes.ALOAD, thisSlot));
                                        } else {
                                            var type = stack.getType();
                                            iterator.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), locals));
                                            locals += type.getSize();
                                        }
                                    }

                                    break;
                                }
                            }


                        }
                    }
                    continue;
                }
                iterator.remove();
            }
        }

        return node;
    }
}
