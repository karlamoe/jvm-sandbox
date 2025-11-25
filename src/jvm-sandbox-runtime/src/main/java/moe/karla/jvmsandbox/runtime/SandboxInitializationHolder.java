package moe.karla.jvmsandbox.runtime;

public class SandboxInitializationHolder {
    public static final ThreadLocal<SandboxRuntime> PROVIDING_RUNTIME = new ThreadLocal<>();
}
