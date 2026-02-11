package com.tissue.feature.member.domain;

public enum SystemRole {
    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
