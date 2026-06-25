package com.tissue.feature.member.domain;

public enum MemberStatus {
    ACTIVE,

    /**
     * Administratively locked by a SUPER_ADMIN. Cannot log in or refresh tokens until unlocked.
     * PII is still present. The lock is fully reversible via unlock.
     */
    LOCKED,

    /**
     * Deleted by the user. Within retention period, PII is still present, and restorable.
     */
    DELETED,

    /**
     * PII has been wiped. The row is kept only as an attribution anchor for related data
     * (issues, comments, etc.).
     */
    PURGED;
}
