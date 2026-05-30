package com.tissue.feature.project.domain.policy;

import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProjectKeyPrefixPolicy {

    public static final Set<String> RESERVED_PREFIXES =
            Set.of("ISSUE", "SPRINT", "TYPE", "FIELD", "STATUS", "TRANSITION", "WORKFLOW", "OPTION");

    public static boolean isReserved(String prefix) {
        return RESERVED_PREFIXES.contains(prefix.toUpperCase(Locale.ENGLISH));
    }
}
