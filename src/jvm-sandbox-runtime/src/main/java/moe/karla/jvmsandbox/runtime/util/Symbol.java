package moe.karla.jvmsandbox.runtime.util;

@SuppressWarnings("RedundantMethodOverride")
public final class Symbol {
    private final String name;
    private final String toString;
    private final int hashCode;

    public Symbol(String name) {
        this.name = name;
        this.toString = "Symbol(" + name + ")";
        this.hashCode = name.hashCode() ^ System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return toString;
    }

    public String name() {
        return name;
    }


    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
