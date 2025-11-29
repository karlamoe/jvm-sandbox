package moe.karla.jvmsandbox.transformer.context;

import moe.karla.jvmsandbox.transformer.interpreter.TransformInterpreter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class TransformContext {
    public TransformInterpreter interpreter = new TransformInterpreter();
    public Map<Attr<?>, Object> attributes = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T attr(Attr<T> attr) {
        return (T) attributes.computeIfAbsent(attr, $ -> $.initializer.get());
    }


    public static class Attr<T> {
        private final Supplier<T> initializer;

        public Attr(Supplier<T> initializer) {
            this.initializer = initializer;
        }
    }
}
