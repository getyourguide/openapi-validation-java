package com.getyourguide.openapi.validation.core.executor;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class VirtualThreadLimitedExecutor implements Executor {
    private static final int DEFAULT_MAX_CONCURRENT = 2;
    private final int maxConcurrent;
    private final AtomicInteger runningCount = new AtomicInteger(0);

    public VirtualThreadLimitedExecutor() {
        this(DEFAULT_MAX_CONCURRENT);
    }

    public VirtualThreadLimitedExecutor(int maxConcurrent) {
        checkVirtualThreadSupport();
        this.maxConcurrent = maxConcurrent;
    }

    public static boolean isSupported() {
        try {
            checkVirtualThreadSupport();
            return true;
        } catch (UnsupportedOperationException | NoSuchMethodError e) {
            return false;
        }
    }

    private static void checkVirtualThreadSupport() {
        // This will throw NoSuchMethodError on Java < 21
        //noinspection ResultOfMethodCallIgnored
        Thread.ofVirtual();
    }

    @Override
    public void execute(Runnable command) {
        if (runningCount.get() >= maxConcurrent) {
            return;
        }

        if (runningCount.incrementAndGet() > maxConcurrent) {
            runningCount.decrementAndGet();
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                command.run();
            } finally {
                runningCount.decrementAndGet();
            }
        });
    }
}
