package com.tissue.mcp.tool;

import com.tissue.shared.auth.MemberDetails;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class McpActor {

    private static final String SCOPE_PREFIX = "SCOPE_";

    private McpActor() {}

    public static MemberDetails current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberDetails principal)) {
            throw new IllegalStateException("MCP tool invoked without an authenticated agent");
        }
        return principal;
    }

    public static Long currentMemberId() {
        return current().getMemberId();
    }

    /**
     * The granted PAT scopes.
     *
     * <p>{@code SCOPE_READ}, {@code SCOPE_WRITE}
     */
    public static List<String> currentScopes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(SCOPE_PREFIX))
                .toList();
    }
}
