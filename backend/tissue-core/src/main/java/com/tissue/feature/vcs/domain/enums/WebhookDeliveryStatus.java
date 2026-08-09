package com.tissue.feature.vcs.domain.enums;

/**
 * Lifecycle of a single inbound webhook delivery.
 *
 * <p>{@code IGNORED} and {@code DEAD} are both terminal but mean different things: {@code IGNORED} is a
 * delivery we understood and deliberately did nothing with (an event type we do not act on, a payload we
 * cannot parse, a branch that names no issue), while {@code DEAD} is one we failed to process and gave up
 * retrying.
 */
public enum WebhookDeliveryStatus {
    RECEIVED,
    PROCESSED,
    IGNORED,
    FAILED,
    DEAD;
}
