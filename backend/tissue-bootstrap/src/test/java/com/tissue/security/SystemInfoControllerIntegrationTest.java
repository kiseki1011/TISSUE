package com.tissue.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tissue.security.config.DeploymentProperties;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SystemInfoControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeploymentProperties deploymentProperties;

    @AfterEach
    void tearDown() {
        deploymentProperties.setMultiTenant(false);
    }

    @Test
    @DisplayName("200: returns multiTenant=false by default")
    void returnsMultiTenantFalseByDefault() throws Exception {
        // given
        deploymentProperties.setMultiTenant(false);

        // when / then
        mockMvc.perform(get("/api/v1/system-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.multiTenant").value(false))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.serverName").exists())
                .andExpect(jsonPath("$.setup").exists());
    }

    @Test
    @DisplayName("200: returns multiTenant=true when configured")
    void returnsMultiTenantTrueWhenConfigured() throws Exception {
        // given
        deploymentProperties.setMultiTenant(true);

        // when / then
        mockMvc.perform(get("/api/v1/system-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.multiTenant").value(true));
    }
}
