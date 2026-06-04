package com.tissue.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tissue.security.application.port.oidc.OidcClient;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "tissue.auth.mode=OIDC",
            "tissue.auth.oidc.issuer-uri=https://idp.example.com",
            "tissue.auth.oidc.client-id=tissue-client"
        })
class OidcModeWebIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OidcClient oidcClient;

    @Test
    @DisplayName("local login is rejected in OIDC mode")
    void localLoginRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"gildong\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("local signup is rejected in OIDC mode")
    void localSignupRejected() throws Exception {
        mockMvc.perform(post("/api/v1/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
