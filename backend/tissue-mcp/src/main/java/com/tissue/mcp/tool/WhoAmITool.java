package com.tissue.mcp.tool;

import com.tissue.shared.auth.MemberDetails;
import java.util.List;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

/**
 * Reports which agent the current connection is authenticated as.
 *
 * <p>It validates the whole pipeline (PAT auth → tool invocation → identity in context) and lets
 * an agent confirm its own identity.
 */
@Component
public class WhoAmITool {

    @McpTool(name = "whoami", description = """
                    Returns the identity of the calling agent. Its member id, username, and granted scopes. \
                    Can use it to confirm which Tissue identity this connection is authenticated as.""")
    public AgentIdentity whoami() {
        MemberDetails principal = McpActor.current();
        return new AgentIdentity(principal.getMemberId(), principal.getUsername(), McpActor.currentScopes());
    }

    public record AgentIdentity(Long memberId, String username, List<String> scopes) {}
}
