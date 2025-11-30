package moe.karla.jvmsandbox.instructment.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

public class InstructedClassLoader extends URLClassLoader {
    private final InstructedEnvironment environment;

    public InstructedClassLoader(InstructedEnvironment environment, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.environment = environment;
    }

    public InstructedClassLoader(InstructedEnvironment environment, URL[] urls) {
        super(urls);
        this.environment = environment;
    }

    public InstructedClassLoader(InstructedEnvironment environment, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
        this.environment = environment;
    }

    public InstructedClassLoader(String name, InstructedEnvironment environment, URL[] urls, ClassLoader parent) {
        super(name, urls, parent);
        this.environment = environment;
    }

    public InstructedClassLoader(String name, InstructedEnvironment environment, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(name, urls, parent, factory);
        this.environment = environment;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.equals(environment.getWrapperClass().getName())) {
            return environment.getWrapperClass();
        }

        String path = name.replace('.', '/').concat(".class");
        var resource = findResource(path);
        if (resource == null) throw new ClassNotFoundException(name);

        // TODO code source, manifest
        try (var inputStream = resource.openStream()) {
            var bytecode = environment.transform(inputStream.readAllBytes());
            return defineClass(name, bytecode, 0, bytecode.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
}
