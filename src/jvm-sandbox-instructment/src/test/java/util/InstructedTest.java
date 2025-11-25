package util;

import moe.karla.jvmsandbox.instructment.generator.HookWrapperGenerator;
import moe.karla.jvmsandbox.instructment.generator.HookWrapperInterpreter;
import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.transformer.TransformContext;
import moe.karla.jvmsandbox.transformer.TransformerChain;
import moe.karla.jvmsandbox.transformer.transformers.AllocPostProcessTransformer;
import moe.karla.jvmsandbox.transformer.transformers.AllocPreProcessTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class InstructedTest {

    SandboxRuntime runtime;

    protected ClassNode targetNode;
    protected Class<?> targetClass;

    @BeforeEach
    void beforeAll() throws Throwable {
        if (runtime != null) return;

        runtime = init();

        var name = (InstructedTest.class.getName() + "." + getClass().getName() + "$TargetClass")
                .replace('.', '/');

        var mw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        HookWrapperGenerator.PROVIDING_RUNTIME.set(runtime);
        HookWrapperGenerator.generate(mw, name);


        var node = new ClassNode();
        new ClassReader(getClass().getName() + "$TargetClass")
                .accept(node, 0);

        TransformContext context = new TransformContext();
        context.interpreter = new HookWrapperInterpreter(name);
        targetNode = transformerChain().transform(node, context);

        var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//        targetNode.accept(new TraceClassVisitor(cw, new PrintWriter(System.out)));
        targetNode.accept(cw);

        new ClassLoader(getClass().getClassLoader()) {{
            var bytecode = mw.toByteArray();
            var klass = defineClass(null, bytecode, 0, bytecode.length);
            Class.forName(klass.getName(), true, this);

            bytecode = cw.toByteArray();
            targetClass = defineClass(null, bytecode, 0, bytecode.length);
        }};
    }

    protected TransformerChain transformerChain() {
        return new TransformerChain(
                List.of(
                        new AllocPreProcessTransformer(),
                        new AllocPostProcessTransformer()
                )
        );
    }

    protected SandboxRuntime init() {
        return new SandboxRuntime();
    }


    @Test
    void $$init() {
    }
}
