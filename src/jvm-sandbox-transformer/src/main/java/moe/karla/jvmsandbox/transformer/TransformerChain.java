package moe.karla.jvmsandbox.transformer;

import org.objectweb.asm.tree.ClassNode;

public class TransformerChain extends Transformer {
    private final Iterable<? extends Transformer> transformers;

    public TransformerChain(Iterable<? extends Transformer> transformers) {
        this.transformers = transformers;
    }

    @Override
    public ClassNode transform(ClassNode node, TransformContext context) throws Throwable {
        for (var transformer : transformers) {
            var newNode = transformer.transform(node, context);

            if (newNode != null) {
                node = newNode;
            }
        }
        return node;
    }
}
