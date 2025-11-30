# Contributing

Thank you for helping jvm-sandbox.

## How this project works

Let's start with the class file.
A class file can be viewed as a set of operation instructions.

There are what we're caring:

- Invoking
    - [invokevirtual](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.invokevirtual)
    - [invokestatic](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.invokestatic)
    - [invokespecial](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.invokespecial)
    - [invokeinterface](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.invokeinterface)
    - [invokedynamic](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.invokedynamic)
- Field accessing
    - [putstatic](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.putstatic)
    - [putfield](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.putfield)
    - [getfield](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.getfield)
    - [getstatic](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.getstatic)
- Dynamic constants
    - [ldc](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.ldc)
    - [CONSTANT_Fieldref, CONSTANT_Methodref, CONSTANT_InterfaceMethodref](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.4.2)
    - [CONSTANT_MethodHandle](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.4.8)
    - [CONSTANT_Dynamic](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.4.10) ([ConstantDynamic](https://asm.ow2.io/javadoc/org/objectweb/asm/ConstantDynamic.html)
      in asm)

All instructions will be transformed with a [Wrapper Class],
so the jvm-sandbox can interpret all instructions.

### Wrapper Class

In order to interpret instructions, it is required to create a wrapper class.

A transformed class invokes a method by using `invokedynamic` with the bootstrap method in Wrapper Class,
which will redirect all requests to real sandbox runtime.

A wrapper class that can be accepted by jvm-sandbox needs to have the following method definitions:

```java
import java.lang.invoke.MethodType;

public class WrapperClass {
    // hook normal calls
    public static CallSite hookNormal(
            MethodHandles.Lookup caller, String dynamicName, MethodType dynamicType,
            Class<?> declaringClass, int refType
    );

    // hook invokedynamic call
    public static /* vararg */ CallSite hookDynamic(
            MethodHandles.Lookup caller, String dynamicName, MethodType dynamicType,
            Class<?> metafactoryOwner, String metafactoryName, MethodType metafactoryType,
            Object... metafactoryArgs
    );

    // hook ConstantDynamic
    public static /* vararg */ Object hookDynamicConstant(
            MethodHandles.Lookup caller, String dynamicName, Class<?> resultType,
            Class<?> metafactoryOwner, String metafactoryName, MethodType metafactoryType,
            Object... metafactoryArgs
    );
}
```

### Class Transformation

By default, jvm-sandbox transform classes with 3 steps:

1. LambdaDeoptimize
2. AllocPreProcess
3. PostProcess

#### LambdaDeoptimize

This step converts all external handle references at the bytecode level into indirect references.

For example:

```java
class Example {
    // original code:
    public void originalMethod() {
        // invokedynamic LambdaMetafactory.metafactory(..., <invokeStatic>Thread.dumpStack()V )
        Runnable task = Thread::dumpStack;
    }

    // transformed code
    public void transformedMethod() {
        // invokedynamic LambdaMetafactory.metafactory(..., <invokeStatic>Example.$$lambda$$deoptimize$$1()V )
        Runnable task = Example::$$lambda$$deoptimize$$1;
    }

    private static void $$lambda$$deoptimize$$1() {
        // invokestatic Thread.dumpStack()V
        Thread.dumpStack();
    }
}

```

This is a very important preprocessing step; without it, it's easy to escape the sandbox.

Since LambdaMetafactory only accepts direct method handles, we chose to create a wrapper method to indirectly
call the original handle.

This enhances security and reduces complexity,
because the generated wrapper method will be processed by subsequent steps as a normal method call.

If we don't do this, we must cast the method handle to ConstantDynamic, which is released at Java 11.  
We also need to consider the special case of calling LambdaMetafactory.

#### AllocPreProcess

> Object Allocation Pre Process

This step is for analyzing the object creation flow, including creating new object (`new Object()`) and super
constructor calling (`super();`).

In bytecode, at least two instructions is required to create a new object.

For example:

```text
NEW java/lang/Object
invokespecial java/lang/Object.<init>()V
```

This is not conducive to bytecode analysis,
so we will first convert the object creation instruction into a special instruction.

This instruction is invalid in the JVM, but compressing it into a single instruction makes it easier for post-processors
to analyze and convert it.

Similar to object creation, we also process the super constructor call instruction so that subsequent processing does
not require re-execution of bytecode analysis.

```text
# Object Creation
| NEW java/lang/Object                        ->  |
| invokespecial java/lang/Object.<init>()V        | invokestatic java/lang/Object.<new>()V

| NEW java/lang/Object                        ->  |
| DUP                                             |
| invokespecial java/lang/Object.<init>()V        | invokestatic java/lang/Object.<new>()Ljava/lang/Object;
| POP                                             | POP


# Super constructor call
| ALOAD 0                                         | ALOAD 0
| .......                                     ->  | .......
| invokespecial java/lang/Object.<init>()V        | invokespecial java/lang/Object.<constructor_invoke>()V
| RETURN                                          | RETURN
```

#### PostProcess

PostProcess is the real transformer.

This processor delegates all critical instructions to the interpreter. The interpreter then replaces these instructions
with invokedynamic instructions with the same semantics, linked to the [Wrapper Class].

### Runtime Linkage

All external calls to a sandboxed class are controlled by [Wrapper Class].

Additionally, the constructor of a sandboxed class will have a
special instruction inserted before calling the parent constructor.

This instruction is

```text
invokedynamic <WrapperClass.hookNormal>(
    "_"                     // method name
    (....)V                 // method descriptor. same as the parent constructor
    <ParentClass>.class     // super class of the sandboxed class
    20201480                // special reference type: [InvokeHelper]#EXREF_beforeConstructor
)
```

See also [InvokeHelper].

#

<!-- alias -->

[Wrapper Class]: #Wrapper-Class

[InvokeHelper]: ./src/jvm-sandbox-runtime/src/main/java/moe/karla/jvmsandbox/runtime/util/InvokeHelper.java
