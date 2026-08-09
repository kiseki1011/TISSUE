package com.tissue.feature.issue.domain.enums;

/**
 * Current state of a linked pull request. Derived from the provider's own state rather than from the event
 * that arrived, so an event Tissue does not otherwise act on (a new commit pushed to the PR, a label
 * change) still leaves the state correct.
 */
public enum PullRequestState {
    OPEN,
    CLOSED,
    MERGED;
}
