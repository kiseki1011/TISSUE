package com.tissue.security.authorization;

public enum SystemRole {

	USER,
	ADMIN;

	public String getAuthority() {
		return "ROLE_" + this.name();
	}
}
