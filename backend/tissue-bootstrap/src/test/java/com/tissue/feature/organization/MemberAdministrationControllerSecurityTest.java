package com.tissue.feature.organization;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tissue.feature.member.web.request.AssignMemberTeamRequest;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@AutoConfigureMockMvc
class MemberAdministrationControllerSecurityTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor as(String authority) {
        MemberDetails principal =
                new MemberDetails(1L, "actor@tissue.com", "actor", List.of(new SimpleGrantedAuthority(authority)));
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("403: a USER cannot reach the @RequireSystemAdmin team assignment endpoint")
    void userIsForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/members/999/team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignMemberTeamRequest(null)))
                        .with(as("ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("404: ADMIN passes the gate but member is missing")
    void adminPassesGate() throws Exception {
        mockMvc.perform(patch("/api/v1/members/999/team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignMemberTeamRequest(null)))
                        .with(as("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("404: SUPER_ADMIN passes the gate but member is missing")
    void superAdminPassesViaRoleHierarchy() throws Exception {
        mockMvc.perform(patch("/api/v1/members/999/team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignMemberTeamRequest(null)))
                        .with(as("ROLE_SUPER_ADMIN")))
                .andExpect(status().isNotFound());
    }
}
