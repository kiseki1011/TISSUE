package com.tissue.feature.issue.application.port.repository;

import java.time.Instant;

/**
 * A start/end instant pair, used to compute a duration in the service rather than in SQL (keeps the
 * percentile maths portable and out of the query layer).
 */
public interface TimestampPairRow {

    Instant getStartAt();

    Instant getEndAt();
}
