package moe.karla.jvmsandbox.instructment.loader;

import moe.karla.jvmsandbox.instructment.generator.HookWrapperInterpreter;
import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.transformer.TransformerChain;
import moe.karla.jvmsandbox.transformer.context.ApplicationTransformContext;
import moe.karla.jvmsandbox.transformer.transformers.AllocPreProcessTransformer;
import moe.karla.jvmsandbox.transformer.transformers.LambdaDeoptimizeTransformer;
import moe.karla.jvmsandbox.transformer.transformers.PostProcessTransformer;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

public class InstructedEnvironment {
    public static final TransformerChain DEFAULT_TRANSFORMER_CHAIN = new TransformerChain(List.of(
            new LambdaDeoptimizeTransformer(),
            new AllocPreProcessTransformer(),
            new PostProcessTransformer()
    ));


    private final @NotNull SandboxRuntime runtime;
    private final @NotNull TransformerChain transformerChain;
    private final @NotNull Class<?> wrapperClass;
    private final @NotNull ApplicationTransformContext context;

    public InstructedEnvironment(
            @NotNull SandboxRuntime runtime,
            @NotNull TransformerChain transformerChain,
            @NotNull Class<?> wrapperClass,
            @NotNull ApplicationTransformContext context
    ) {
        this.runtime = runtime;
        this.transformerChain = transformerChain;
        this.wrapperClass = wrapperClass;
        this.context = context;
    }

    public InstructedEnvironment(
            @NotNull SandboxRuntime runtime,
            @NotNull Class<?> wrapperClass
    ) {
        this.runtime = runtime;
        this.transformerChain = DEFAULT_TRANSFORMER_CHAIN;
        this.wrapperClass = wrapperClass;
        this.context = new ApplicationTransformContext();
        this.context.interpreter = new HookWrapperInterpreter(wrapperClass.getName().replace('.', '/'));
    }


    public ClassNode transform(ClassNode klass) {
        try {
            return transformerChain.transform(klass, context);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] transform(byte[] klass) {
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        var node = new ClassNode();
        new ClassReader(klass).accept(node, 0);

        transform(node).accept(writer);

        return writer.toByteArray();
    }


    public @NotNull SandboxRuntime getRuntime() {
        return runtime;
    }

    public @NotNull TransformerChain getTransformerChain() {
        return transformerChain;
    }

    public @NotNull Class<?> getWrapperClass() {
        return wrapperClass;
    }

    public @NotNull ApplicationTransformContext getContext() {
        return context;
    }
}
