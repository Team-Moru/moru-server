package com.moru.server.global.logging;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class SanitizedLogException extends RuntimeException {

    private static final int MAX_CAUSE_DEPTH = 8;

    private SanitizedLogException(Throwable source, Throwable sanitizedCause) {
        super(source.getClass().getName(), sanitizedCause, false, false);
    }

    public static Throwable from(Throwable source) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return copy(source, visited, 0);
    }

    private static Throwable copy(Throwable source, Set<Throwable> visited, int depth) {
        if (source == null || depth >= MAX_CAUSE_DEPTH || !visited.add(source)) {
            return null;
        }

        Throwable sanitizedCause = copy(source.getCause(), visited, depth + 1);
        return new SanitizedLogException(source, sanitizedCause);
    }
}
