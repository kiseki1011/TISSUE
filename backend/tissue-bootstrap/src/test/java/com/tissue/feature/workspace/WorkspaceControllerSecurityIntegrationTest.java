package com.tissue.feature.workspace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.workspace.web.request.CreateWorkspaceRequest;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.service.TokenPairCreateService;
import com.tissue.security.config.DeploymentProperties;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
class WorkspaceControllerSecurityIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private TokenPairCreateService tokenPairCreateService;

    @Autowired
    private DeploymentProperties deploymentProperties;

    @AfterEach
    void tearDown() {
        deploymentProperties.setMultiTenant(false);
    }

    @Test
    @DisplayName("403: ROLE_USER cannot create workspace in single-tenant mode")
    void roleUserBlockedInSingleTenant() throws Exception {
        // given
        deploymentProperties.setMultiTenant(false);
        String token = createMemberAndIssueToken("user@tissue.com", "regularuser", SystemRole.USER);

        // when & then
        mockMvc.perform(createWorkspaceRequest("user-ws", token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("201: ROLE_ADMIN can create workspace in single-tenant mode")
    void roleAdminAllowedInSingleTenant() throws Exception {
        // given
        deploymentProperties.setMultiTenant(false);
        String token = createMemberAndIssueToken("admin@tissue.com", "adminuser", SystemRole.ADMIN);

        // when & then
        mockMvc.perform(createWorkspaceRequest("admin-ws", token)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("201: ROLE_USER can create workspace in multi-tenant mode")
    void roleUserAllowedInMultiTenant() throws Exception {
        // given
        deploymentProperties.setMultiTenant(true);
        String token = createMemberAndIssueToken("user2@tissue.com", "regularuser2", SystemRole.USER);

        // when & then
        mockMvc.perform(createWorkspaceRequest("multi-ws", token)).andExpect(status().isCreated());
    }

    private String createMemberAndIssueToken(String email, String username, SystemRole role) {
        Member member = role == SystemRole.ADMIN
                ? Member.createAsAdmin(email, username, "Test User")
                : Member.create(email, username, "Test User");
        Member saved = memberCommandRepository.save(member);

        var authorities = List.of(new SimpleGrantedAuthority(saved.getRole().getAuthority()));
        TokenPair tokens =
                tokenPairCreateService.createTokens(saved.getId(), saved.getEmail(), saved.getUsername(), authorities);
        return tokens.accessToken();
    }

    private MockHttpServletRequestBuilder createWorkspaceRequest(String workspaceKey, String accessToken)
            throws Exception {
        var body = new CreateWorkspaceRequest(workspaceKey, "Test Workspace", "desc");
        return post("/api/v1/workspaces")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }
}
