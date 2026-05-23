package com.tissue.feature.member.domain;

public enum MemberStatus {
    ACTIVE,

    LOCKED,

    /**
     * Deleted by the user. Within retention period, PII is still present,
     * and restorable.
     */
    DELETED,

    /**
     * PII has been wiped. The row is kept only as an attribution anchor for
     * related data (issues, comments, etc.).
     */
    PURGED;
}
