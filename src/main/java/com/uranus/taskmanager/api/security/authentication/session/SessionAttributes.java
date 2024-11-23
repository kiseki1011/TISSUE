package com.uranus.taskmanager.api.security.authentication.session;

public final class SessionAttributes {
	/**
	 * Login related
	 */
	public static final String LOGIN_MEMBER_ID = "id";
	public static final String LOGIN_MEMBER_LOGIN_ID = "loginId";
	public static final String LOGIN_MEMBER_EMAIL = "email";

	/**
	 * Update authorization related
	 */
	public static final String UPDATE_AUTH = "UPDATE_AUTH";
	public static final String UPDATE_AUTH_EXPIRES_AT = "UPDATE_AUTH_EXPIRES_AT";

	private SessionAttributes() {
	}
}
