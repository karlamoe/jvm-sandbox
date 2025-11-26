package moe.karla.jvmsandbox.runtime.util;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.TypeDescriptor;
import java.lang.reflect.Member;

/// @param relatedMember Indicated from MethodHandles.Lookup#unreflect XXX
public record RuntimeResolvationInfo(
        @Nullable TypeDescriptor typeDescriptor,

        @Nullable Member relatedMember,
        @Nullable Class<?> specialCaller
) {

}
