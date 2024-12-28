package com.tissue.api.security.session;

public final class SessionAttributes {

	/**
	 * Login related
	 */
	public static final String LOGIN_MEMBER_ID = "id";
	public static final String LOGIN_MEMBER_LOGIN_ID = "loginId";
	public static final String LOGIN_MEMBER_EMAIL = "email";

	/**
	 * Member Update authorization related
	 */
	public static final String MEMBER_UPDATE_AUTH = "MEMBER_UPDATE_AUTH";
	public static final String MEMBER_UPDATE_AUTH_EXPIRES_AT = "MEMBER_UPDATE_AUTH_EXPIRES_AT";

	/**
	 * Member Delete authorization related
	 */
	public static final String MEMBER_DELETE_AUTH = "MEMBER_DELETE_AUTH";
	public static final String MEMBER_DELETE_AUTH_EXPIRES_AT = "MEMBER_DELETE_AUTH_EXPIRES_AT";

	private SessionAttributes() {
	}
}
