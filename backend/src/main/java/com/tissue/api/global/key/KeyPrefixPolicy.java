package com.tissue.api.global.key;

import java.util.Locale;
import java.util.Set;

public class KeyPrefixPolicy {

	public static final String WORKSPACE = "WS";
	public static final String ISSUE = "ISSUE";
	public static final String SPRINT = "SPRINT";
	public static final String ISSUE_TYPE = "TYPE";
	public static final String ISSUE_FIELD = "FIELD";
	public static final String ISSUE_ENUM_FIELD_OPTION = "OPTION";
	public static final String WORKFLOW = "WF";
	public static final String STATUS = "STATUS";
	public static final String TRANSITION = "TRANSITION";

	/**
	 * Issue can use "ISSUE" as prefix for key
	 */
	public static final Set<String> RESERVED_PREFIXES = Set.of(
		WORKSPACE, SPRINT, ISSUE_TYPE, ISSUE_FIELD, WORKFLOW, STATUS, TRANSITION, ISSUE_ENUM_FIELD_OPTION,
		"WORKSPACE", "WORKFLOW"
		// , ISSUE
	);

	public static boolean isReserved(String prefix) {
		return RESERVED_PREFIXES.contains(prefix.toUpperCase(Locale.ENGLISH));
	}

	public static String format(String prefix, long identifier) {
		return prefix.toUpperCase(Locale.ENGLISH) + "-" + identifier;
	}

	public static String format(String prefix, String identifier) {
		return prefix.toUpperCase(Locale.ENGLISH) + "-" + identifier;
	}
}
