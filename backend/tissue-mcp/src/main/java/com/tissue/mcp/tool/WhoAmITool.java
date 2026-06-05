package com.tissue.mcp.tool;

import com.tissue.shared.auth.MemberDetails;
import java.util.List;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reports which agent the current connection is authenticated as.
 *
 * <p>It validates the whole pipeline (PAT auth → tool invocation → identity in context) and lets
 * an agent confirm its own identity.
 */
@Component
public class WhoAmITool {

    @McpTool(
            name = "whoami",
            description = "Returns the identity of the calling agent. Its member id, username, and granted scopes. "
                    + "Can use it to confirm which Tissue identity this connection is authenticated as.")
    public AgentIdentity whoami() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberDetails principal)) {
            throw new IllegalStateException("MCP tool invoked without an authenticated agent");
        }

        List<String> scopes = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("SCOPE_"))
                .toList();

        return new AgentIdentity(principal.getMemberId(), principal.getUsername(), scopes);
    }

    public record AgentIdentity(Long memberId, String username, List<String> scopes) {}
}
