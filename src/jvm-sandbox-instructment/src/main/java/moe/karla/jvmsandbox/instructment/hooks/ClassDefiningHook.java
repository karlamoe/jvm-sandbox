package moe.karla.jvmsandbox.instructment.hooks;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.hooks.FakedInvocationHook;
import moe.karla.jvmsandbox.runtime.util.InvokeHelper;
import moe.karla.jvmsandbox.runtime.util.RuntimeResolvationInfo;
import moe.karla.jvmsandbox.transformer.TransformerChain;
import moe.karla.jvmsandbox.transformer.context.ApplicationTransformContext;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ClassDefiningHook extends FakedInvocationHook {

    private static final MethodHandle MH_BYTEARRAY_TRANSFORM;
    private static final MethodHandle MH_BYTEBUFFER_TRANSFORM;
    private static final MethodHandle MH_BYTEARRAY_LENGTH;
    private static final MethodHandle MH_COPY_ARRAY;

    static {
        try {
            var lookup = MethodHandles.lookup();
            MH_BYTEARRAY_TRANSFORM = lookup.findVirtual(ClassDefiningHook.class, "transform", MethodType.methodType(byte[].class, byte[].class));
            MH_BYTEBUFFER_TRANSFORM = lookup.findVirtual(ClassDefiningHook.class, "transform", MethodType.methodType(ByteBuffer.class, ByteBuffer.class));
            MH_BYTEARRAY_LENGTH = MethodHandles.arrayLength(byte[].class);
            MH_COPY_ARRAY = lookup.findStatic(ClassDefiningHook.class, "copyArray", MethodType.methodType(byte[].class, byte[].class, int.class, int.class));
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }


    private final @NotNull TransformerChain transformerChain;
    private final @NotNull ApplicationTransformContext context;
    private final MethodHandle mh$bytearray$transform;
    private final MethodHandle mh$bytebuffer$transform;

    public ClassDefiningHook(@NotNull TransformerChain transformerChain, @NotNull ApplicationTransformContext context) {
        this.transformerChain = transformerChain;
        this.context = context;
        mh$bytearray$transform = MH_BYTEARRAY_TRANSFORM.bindTo(this);
        mh$bytebuffer$transform = MH_BYTEBUFFER_TRANSFORM.bindTo(this);
    }

    @Override
    public MethodHandle interpretInvoke0(
            SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> owner,
            String methodName, MethodType desc, int refType,
            RuntimeResolvationInfo callInfo
    ) throws Throwable {
        if (owner == MethodHandles.Lookup.class) {
            switch (methodName) {
                case "defineClass", "defineHiddenClass", "defineHiddenClassWithClassData" -> {
                    var realMh = InvokeHelper.resolveMethodHandle(caller, owner, methodName, desc, refType, callInfo);
                    return MethodHandles.filterArguments(realMh, 0, mh$bytearray$transform);
                }
            }
        }
        if (ClassLoader.class.isAssignableFrom(owner)) {
            if ("defineClass".equals(methodName)) {
                var mh = InvokeHelper.resolveMethodHandle(caller, owner, methodName, desc, refType, callInfo);

                var offsetBase = desc.parameterType(1) == String.class ? 2 : 1;
                var bytebuf = desc.parameterType(offsetBase) == java.nio.ByteBuffer.class;

                if (bytebuf) {
                    return MethodHandles.filterArguments(
                            mh,
                            offsetBase,
                            mh$bytebuffer$transform
                    );
                } else {
                    // assert: offsetBase  = 2
                    //     0            1       2       3        4        5
                    //mh: (ClassLoader, String, byte[], int off, int len, ProtectionDomain)

                    mh = MethodHandles.insertArguments(
                            mh, offsetBase + 1,
                            0
                    );

                    //     0            1       2       3        4
                    //mh: (ClassLoader, String, byte[], int len, ProtectionDomain)


                    mh = MethodHandles.filterArguments(
                            mh, offsetBase + 1,
                            MH_BYTEARRAY_LENGTH
                    );
                    //     0            1       2       3       4
                    //mh: (ClassLoader, String, byte[], byte[], ProtectionDomain)

                    var order = new int[mh.type().parameterCount()];
                    for (var i = 0; i < order.length; i++) {
                        order[i] = i;
                        if (i > offsetBase) {
                            order[i] = i - 1;
                        }
                    }

                    mh = MethodHandles.permuteArguments(
                            mh,
                            mh.type().dropParameterTypes(offsetBase + 1, offsetBase + 2),
                            order
                    );
                    //     0            1       2       3
                    //mh: (ClassLoader, String, byte[], ProtectionDomain)

                    mh = MethodHandles.filterArguments(mh, offsetBase, mh$bytearray$transform);

                    // casting
                    mh = MethodHandles.collectArguments(mh, offsetBase, MH_COPY_ARRAY);
                    //     0            1       2       3    4    5
                    //mh: (ClassLoader, String, byte[], int, int, ProtectionDomain)

                    return mh;
                }
            }
        }
        return null;
    }

    private ByteBuffer transform(ByteBuffer klass) throws Throwable {
        byte[] array = new byte[klass.remaining()];
        klass.get(array);
        return ByteBuffer.wrap(transform(array));
    }

    private byte[] transform(byte[] klass) throws Throwable {
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        var node = new ClassNode();
        new ClassReader(klass).accept(node, 0);

        transformerChain.transform(node, context).accept(writer);

        return writer.toByteArray();
    }

    private static byte[] copyArray(byte[] array, int off, int len) {
        if (off == 0 && len == array.length) return array;
        return Arrays.copyOfRange(array, off, off + len);
    }
}
