package com.tissue.feature.notification.sender;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmailThrottledExecutor implements Executor {

    private final Executor delegate;
    private final Semaphore semaphore;
    private final long timeoutSeconds;

    public EmailThrottledExecutor(int maxConcurrency) {
        this.delegate = Executors.newVirtualThreadPerTaskExecutor();
        this.semaphore = new Semaphore(maxConcurrency);
        this.timeoutSeconds = 10;
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(() -> {
            boolean acquired = false;
            try {
                acquired = semaphore.tryAcquire(timeoutSeconds, TimeUnit.SECONDS);
                if (acquired) {
                    command.run();
                } else {
                    log.warn(
                            "Email throttle timeout. timeout={}s, waiting={}",
                            timeoutSeconds,
                            semaphore.getQueueLength());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Email throttle task interrupted", e);
            } catch (Exception e) {
                log.error("Error during email sending", e);
            } finally {
                if (acquired) {
                    semaphore.release();
                }
            }
        });
    }
}
