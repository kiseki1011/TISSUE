package com.uranus.taskmanager.api.security.session;

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

	/**
	 * Session Audit related
	 */
	public static final String CURRENT_WORKSPACE_CODE = "CURRENT_WORKSPACE_CODE";
	public static final String CURRENT_WORKSPACE_MEMBER_ID = "CURRENT_WORKSPACE_MEMBER_ID";

	private SessionAttributes() {
	}
}
