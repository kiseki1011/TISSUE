package com.tissue.api.project.domain.policy;

import java.util.Locale;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProjectKeyPrefixPolicy {

	// TODO: 정책이기 때문에 설정값에서 읽어오도록 변경할까? 아니면 어차피 변경 가능성이 낮으니깐 그대로 둘까?
	public static final Set<String> RESERVED_PREFIXES = Set.of(
		"WS", "ISSUE", "SPRINT", "TYPE", "FIELD", "STATUS", "TRANSITION",
		"WORKSPACE", "WORKFLOW", "OPTION"
	);

	public static boolean isReserved(String prefix) {
		return RESERVED_PREFIXES.contains(prefix.toUpperCase(Locale.ENGLISH));
	}
}
