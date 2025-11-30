package moe.karla.jvmsandbox.runtime.util;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReflectionCache {
    private final Map<MethodHandle, MethodHandle> handlesForwarder = new WeakHashMap<>();
    private final Map<MethodHandle, Callable<MethodHandle>> handlesFaker = new WeakHashMap<>();

    private final ReadWriteLock fakerLocker = new ReentrantReadWriteLock();

    public MethodHandle getFakedRealHandle(MethodHandle handle) throws Exception {
        fakerLocker.readLock().lock();
        try {
            while (true) {
                var directResult = handlesForwarder.get(handle);
                if (directResult != null && directResult != handle) {
                    handle = directResult;
                    continue;
                }

                var supplier = handlesFaker.get(handle);
                if (supplier != null) {
                    fakerLocker.writeLock().lock();
                    try {
                        var newHandle = supplier.call();
                        handlesForwarder.put(handle, newHandle);
                        handlesFaker.remove(handle); // drop supplier as already faked
                        handle = newHandle;
                        continue;
                    } finally {
                        fakerLocker.writeLock().unlock();
                    }
                }

                // no forwarder

                return handle;
            }
        } finally {
            fakerLocker.readLock().unlock();
        }
    }

    public void pushFakedSource(MethodHandle result, MethodHandle source) {
        if (result == source) return;

        fakerLocker.writeLock().lock();
        try {
            handlesForwarder.put(result, source);
        } finally {
            fakerLocker.writeLock().unlock();
        }
    }

    public void pushFakedSource(MethodHandle result, Callable<MethodHandle> source) {
        fakerLocker.writeLock().lock();
        try {
            handlesFaker.put(result, source);
        } finally {
            fakerLocker.writeLock().unlock();
        }
    }
}
