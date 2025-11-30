package moe.karla.jvmsandbox.transformer.context;

import moe.karla.jvmsandbox.transformer.interpreter.TransformInterpreter;

public class ApplicationTransformContext extends TransformContext {
    public TransformInterpreter interpreter = new TransformInterpreter();
}
