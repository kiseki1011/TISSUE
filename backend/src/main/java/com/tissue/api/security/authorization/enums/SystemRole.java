package com.tissue.api.security.authorization.enums;

public enum SystemRole {

	USER,
	ADMIN;

	public String getAuthority() {
		return "ROLE_" + this.name();
	}
}
