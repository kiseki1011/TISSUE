package com.tissue.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tissue.shared.auth.MemberDetails;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@AutoConfigureMockMvc
class AdminAuthorizationSecurityTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor as(String authority) {
        MemberDetails principal =
                new MemberDetails(1L, "actor@tissue.com", "actor", List.of(new SimpleGrantedAuthority(authority)));
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void assertSuperAdminOnly(String path, ResultMatcher superAdminExpected) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path).with(as("ROLE_USER"))).andExpect(status().isForbidden());
        mockMvc.perform(get(path).with(as("ROLE_ADMIN"))).andExpect(status().isForbidden());
        mockMvc.perform(get(path).with(as("ROLE_SUPER_ADMIN"))).andExpect(superAdminExpected);
    }

    @Test
    @DisplayName("200: GET /api/v1/admin/system-info - SUPER_ADMIN only")
    void systemInfoGate() throws Exception {
        assertSuperAdminOnly("/api/v1/admin/system-info", status().isOk());
    }

    @Test
    @DisplayName("200: GET /api/v1/admin/members - SUPER_ADMIN only")
    void memberDirectoryGate() throws Exception {
        assertSuperAdminOnly("/api/v1/admin/members", status().isOk());
    }

    @Test
    @DisplayName("200: GET /api/v1/admin/audit - SUPER_ADMIN only")
    void auditGate() throws Exception {
        assertSuperAdminOnly("/api/v1/admin/audit", status().isOk());
    }

    @Test
    @DisplayName("200: GET /api/v1/admin/activity-logs - SUPER_ADMIN only")
    void activityLogGate() throws Exception {
        assertSuperAdminOnly("/api/v1/admin/activity-logs", status().isOk());
    }

    @Test
    @DisplayName("404: GET /api/v1/admin/projects/{projectKey}/hard/preview: SUPER_ADMIN only (404 once past the gate)")
    void projectPreviewGate() throws Exception {
        assertSuperAdminOnly("/api/v1/admin/projects/NOT-FOUND/hard/preview", status().isNotFound());
    }
}
