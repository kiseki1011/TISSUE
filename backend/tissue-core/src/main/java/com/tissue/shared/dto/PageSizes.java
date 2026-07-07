package com.tissue.shared.dto;

import org.springframework.data.domain.PageRequest;

/**
 * Shared page-size bounds for the query services.
 *
 * <p>Clamps a client-requested size into a sane range so a request can neither ask for an unbounded
 * page nor a zero/negative size that would fail {@link PageRequest}.
 */
public final class PageSizes {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private PageSizes() {}

    /** Clamp a requested size to {@code [1, MAX_PAGE_SIZE]}, defaulting a non-positive size. */
    public static int clamp(int requested) {
        if (requested < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }

    /** A zero-safe {@link PageRequest} using {@link #clamp(int)} for the size. */
    public static PageRequest clampedPageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), clamp(size));
    }
}
