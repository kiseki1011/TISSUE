package com.tissue.feature.vcs.application.dto;

/**
 * Outcome of processing one webhook event. {@code handled=false} means the event was understood but
 * deliberately not acted on, which is a normal result rather than a failure: retrying it would change
 * nothing. The detail is kept verbatim on the delivery record so an operator can see why a push or PR did
 * not attach to an issue without reading server logs.
 */
public record VcsEventResult(boolean handled, String detail) {

    public static VcsEventResult handled(String detail) {
        return new VcsEventResult(true, detail);
    }

    public static VcsEventResult skipped(String detail) {
        return new VcsEventResult(false, detail);
    }
}
