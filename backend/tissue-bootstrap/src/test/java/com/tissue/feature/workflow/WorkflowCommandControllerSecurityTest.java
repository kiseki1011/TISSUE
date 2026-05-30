package com.tissue.feature.workflow;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tissue.security.principal.MemberDetails;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Verifies that {@code @RequireSystemAdmin} (method security) actually gates the global-resource
 * write endpoints, and that the {@code RoleHierarchy} bean lets {@code SUPER_ADMIN} satisfy
 * {@code hasRole('ADMIN')}. A non-existent workflow id is used so a passing authorization check
 * surfaces as 404 (not found) rather than 403 (forbidden).
 */
@AutoConfigureMockMvc
class WorkflowCommandControllerSecurityTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor as(String authority) {
        MemberDetails principal =
                new MemberDetails(1L, "actor@tissue.com", "actor", List.of(new SimpleGrantedAuthority(authority)));
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("403: a USER cannot reach a @RequireSystemAdmin command endpoint")
    void userIsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/workflows/999999").with(as("ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN passes the gate (404 for the missing workflow, not 403)")
    void adminPassesGate() throws Exception {
        mockMvc.perform(delete("/api/v1/workflows/999999").with(as("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SUPER_ADMIN passes the gate via role hierarchy (404, not 403)")
    void superAdminPassesViaRoleHierarchy() throws Exception {
        mockMvc.perform(delete("/api/v1/workflows/999999").with(as("ROLE_SUPER_ADMIN")))
                .andExpect(status().isNotFound());
    }
}
