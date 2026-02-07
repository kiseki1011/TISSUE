package com.tissue.global.async;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThrottledExecutor implements Executor {

    private final Executor delegate;
    private final Semaphore semaphore;

    public ThrottledExecutor(int maxConcurrency) {
        this.delegate = Executors.newVirtualThreadPerTaskExecutor();
        this.semaphore = new Semaphore(maxConcurrency);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(() -> {
            try {
                semaphore.acquire();
                try {
                    command.run();
                } finally {
                    semaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("ThrottledExecutor task interrupted", e);
            } catch (Exception e) {
                log.error("ThrottledExecutor task failed", e);
            }
        });
    }
}
