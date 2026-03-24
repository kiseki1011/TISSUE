package com.tissue.security.application.port.repository;

import java.time.Duration;

public interface RateLimitStore {

    int incrementAndGet(String key, Duration window);

    int getCount(String key);

    void reset(String key);
}
