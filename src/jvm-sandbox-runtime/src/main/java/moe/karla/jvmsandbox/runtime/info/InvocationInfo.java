package moe.karla.jvmsandbox.runtime.info;

public interface InvocationInfo {
    public int getReferenceKind();

    public String getDeclaringClass();

    public String getName();

    public String getDescriptor();
}
