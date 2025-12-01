package moe.karla.jvmsandbox.runtime.helper;

import moe.karla.jvmsandbox.runtime.SandboxRuntime;
import moe.karla.jvmsandbox.runtime.hooks.FakedInvocationHook;
import moe.karla.jvmsandbox.runtime.util.RuntimeResolvationInfo;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.StringTokenizer;

public class ProcessExecutionHook extends FakedInvocationHook {
    @Override
    public MethodHandle interpretInvoke0(SandboxRuntime runtime, MethodHandles.Lookup caller, Class<?> owner, String methodName, MethodType desc, int refType, RuntimeResolvationInfo callInfo) throws Throwable {

        if (owner == Runtime.class) {
            if ("exec".equals(methodName)) {
                var mh = MethodHandles.lookup().findVirtual(ProcessExecutionHook.class, "hookRuntimeExec",
                        desc.changeParameterType(0, MethodHandles.Lookup.class)
                );
                mh = MethodHandles.insertArguments(mh, 0, this, caller);
                return MethodHandles.dropArguments(mh, 0, desc.parameterType(0));
            }
        }
        if (owner == ProcessBuilder.class) {
            if ("start".equals(methodName)) {
                var mh = MethodHandles.lookup().findVirtual(
                        ProcessExecutionHook.class, "hookProcessBuilderStart", desc.insertParameterTypes(0, MethodHandles.Lookup.class)
                );
                return MethodHandles.insertArguments(mh, 0, this, caller);
            }
            if ("startPipeline".equals(methodName)) {
                var mh = MethodHandles.lookup().findVirtual(
                        ProcessExecutionHook.class, "hookProcessBuilderStartPipeline", desc.insertParameterTypes(0, MethodHandles.Lookup.class)
                );
                return MethodHandles.insertArguments(mh, 0, this, caller);
            }
        }

        if (owner == ProcessHandle.class) {
            if ("destroy".equals(methodName)) {
                var mh = MethodHandles.lookup().findVirtual(
                        ProcessExecutionHook.class, "hookProcessHandleDestroy",
                        desc.insertParameterTypes(0, MethodHandles.Lookup.class, boolean.class)
                );
                return MethodHandles.insertArguments(mh, 0, this, caller, false);
            }
            if ("destroyForcibly".equals(methodName)) {
                var mh = MethodHandles.lookup().findVirtual(
                        ProcessExecutionHook.class, "hookProcessHandleDestroy",
                        desc.insertParameterTypes(0, MethodHandles.Lookup.class, boolean.class)
                );
                return MethodHandles.insertArguments(mh, 0, this, caller, true);
            }
        }

        if (owner == Process.class) {
            if ("destroy".equals(methodName)) {
                var mh = MethodHandles.lookup().findVirtual(
                        ProcessExecutionHook.class, "hookProcessDestroy",
                        desc.insertParameterTypes(0, MethodHandles.Lookup.class, boolean.class)
                );
                return MethodHandles.insertArguments(mh, 0, this, caller, false);
            }
            if ("destroyForcibly".equals(methodName)) {
                var mh = MethodHandles.lookup().findVirtual(
                        ProcessExecutionHook.class, "hookProcessDestroy",
                        desc.insertParameterTypes(0, MethodHandles.Lookup.class, boolean.class)
                                .changeReturnType(void.class)
                );
                mh = MethodHandles.insertArguments(mh, 0, this, caller, true);

                return MethodHandles.foldArguments(
                        MethodHandles.identity(Process.class),
                        0,
                        mh
                );

            }
        }

        return null;
    }

    protected boolean hookProcessHandleDestroy(MethodHandles.Lookup caller, boolean forcibly, ProcessHandle processHandle) {
        if (forcibly) {
            return processHandle.destroyForcibly();
        } else {
            return processHandle.destroy();
        }
    }

    protected void hookProcessDestroy(MethodHandles.Lookup caller, boolean forcibly, Process process) {
        if (forcibly) {
            process.destroyForcibly();
        } else {
            process.destroy();
        }
    }

    protected List<Process> hookProcessBuilderStartPipeline(MethodHandles.Lookup caller, List<ProcessBuilder> builders) throws Throwable {
        return ProcessBuilder.startPipeline(builders);
    }

    protected Process hookProcessBuilderStart(MethodHandles.Lookup caller, ProcessBuilder builder) throws Throwable {
        return builder.start();
    }

    protected Process hookRuntimeExec(MethodHandles.Lookup caller, String[] cmdarray, String[] envp, File dir) throws Throwable {
        var processBuilder = new ProcessBuilder(cmdarray).directory(dir);
        if (envp != null) {
            processBuilder.environment().clear();

            for (var line : envp) {
                if (line.indexOf((int) '\u0000') != -1)
                    line = line.replaceFirst("\u0000.*", "");

                int eqlsign = line.indexOf('=', 1);
                // Silently ignore envstrings lacking the required `='.
                if (eqlsign != -1)
                    processBuilder.environment().put(line.substring(0, eqlsign), line.substring(eqlsign + 1));
            }
        }

        return hookProcessBuilderStart(caller, processBuilder);
    }

    protected Process hookRuntimeExec(MethodHandles.Lookup caller, String command) throws Throwable {
        return hookRuntimeExec(caller, command, null, null);
    }

    protected Process hookRuntimeExec(MethodHandles.Lookup caller, String command, String[] envp) throws Throwable {
        return hookRuntimeExec(caller, command, envp, null);
    }

    protected Process hookRuntimeExec(MethodHandles.Lookup caller, String command, String[] envp, File dir)
            throws Throwable {
        if (command.isEmpty())
            throw new IllegalArgumentException("Empty command");

        StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++)
            cmdarray[i] = st.nextToken();
        return hookRuntimeExec(caller, cmdarray, envp, dir);
    }

    protected Process hookRuntimeExec(MethodHandles.Lookup caller, String[] cmdarray) throws Throwable {
        return hookRuntimeExec(caller, cmdarray, null, null);
    }

    protected Process hookRuntimeExec(MethodHandles.Lookup caller, String[] cmdarray, String[] envp) throws Throwable {
        return hookRuntimeExec(caller, cmdarray, envp, null);
    }

}
