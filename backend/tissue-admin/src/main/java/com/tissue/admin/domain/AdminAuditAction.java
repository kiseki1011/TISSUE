package com.tissue.admin.domain;

public enum AdminAuditAction {
    CHANGE_SYSTEM_ROLE,
    FORCE_WITHDRAW,
    FORCE_RESTORE,
    REVOKE_SESSIONS,
    HARD_DELETE_PROJECT,
    LOCK_MEMBER,
    UNLOCK_MEMBER,
    PURGE_MEMBER,
    FORCE_PASSWORD_RESET
}
