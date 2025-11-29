package util;

import moe.karla.jvmsandbox.instructment.generator.HookWrapperGenerator;
import moe.karla.jvmsandbox.instructment.generator.HookWrapperInterpreter;
import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.transformer.TransformerChain;
import moe.karla.jvmsandbox.transformer.context.ApplicationTransformContext;
import moe.karla.jvmsandbox.transformer.transformers.AllocPostProcessTransformer;
import moe.karla.jvmsandbox.transformer.transformers.AllocPreProcessTransformer;
import moe.karla.jvmsandbox.transformer.transformers.LambdaDeoptimizeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class InstructedTest {

    SandboxRuntime runtime;

    protected ClassNode targetNode;
    protected Class<?> targetClass;

    protected void setupTargetNode(ClassNode node) throws Throwable {
        new ClassReader(getClass().getName() + "$TargetClass")
                .accept(node, 0);

        node.innerClasses = new ArrayList<>();
        node.nestHostClass = null;
        node.nestMembers = new ArrayList<>();

    }

    @BeforeEach
    void beforeAll() throws Throwable {
        if (runtime != null) return;

        runtime = init();
        setup(runtime);

        var name = (InstructedTest.class.getName() + "." + getClass().getName() + "$TargetClass")
                .replace('.', '/');

        var mw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        HookWrapperGenerator.PROVIDING_RUNTIME.set(runtime);
        HookWrapperGenerator.generate(mw, name);


        var node = new ClassNode();
        setupTargetNode(node);

        var context = new ApplicationTransformContext();
        context.interpreter = new HookWrapperInterpreter(name);
        targetNode = transformerChain().transform(node, context);

        var result = new StringWriter();
        var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//        targetNode.accept(new TraceClassVisitor(cw, new PrintWriter(System.out)));
        targetNode.accept(new TraceClassVisitor(cw, new PrintWriter(result)));

        var resultsDir = Paths.get("build/transformed/unit");
        var resultsFile = resultsDir.resolve(getClass().getName() + ".txt");
        Files.createDirectories(resultsFile.getParent());
        Files.writeString(resultsFile, result.toString());


        new ClassLoader(getClass().getClassLoader()) {{
            var bytecode = mw.toByteArray();
            var klass = defineClass(null, bytecode, 0, bytecode.length);
            Class.forName(klass.getName(), true, this);

            bytecode = cw.toByteArray();
            Files.write(
                    resultsDir.resolve(InstructedTest.this.getClass().getName() + ".class"),
                    bytecode
            );
            targetClass = defineClass(null, bytecode, 0, bytecode.length);
        }};
    }

    protected TransformerChain transformerChain() {
        return new TransformerChain(
                List.of(
                        new LambdaDeoptimizeTransformer(),
                        new AllocPreProcessTransformer(),
                        new AllocPostProcessTransformer()
                )
        );
    }

    protected SandboxRuntime init() {
        return new SandboxRuntime();
    }

    protected void setup(SandboxRuntime runtime) {
    }

    @Test
    protected void $$execute() throws Throwable {
        if (Runnable.class.isAssignableFrom(targetClass)) {
            ((Runnable) targetClass.newInstance()).run();
        } else if (Callable.class.isAssignableFrom(targetClass)) {
            ((Callable) targetClass.newInstance()).call();
        } else {
            try {
                var met = targetClass.getDeclaredMethod("run");
                met.setAccessible(true);
                met.invoke(null);
            } catch (NoSuchMethodException ignored) {
            }
        }
    }
}
