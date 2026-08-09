package com.tissue.feature.vcs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves the rate limit is actually attached to the webhook path. The service-level test only shows the
 * budget arithmetic; whether the interceptor's path pattern matches the real endpoint can only be seen by
 * calling it.
 *
 * <p>Uses its own small budget so the assertion needs a handful of calls rather than the production
 * ceiling, and so the counter it fills is not shared with the rest of the suite.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "tissue.security.rate-limit.webhook.max-attempts=3")
class WebhookRateLimitIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private static final String WEBHOOK_PATH = "/api/v1/projects/PROJ/integrations/github/webhook";

    @Test
    @DisplayName("success: calls past the budget are rejected with 429")
    void rejectsCallsPastBudget() throws Exception {
        // given: the budget is spent on calls that are themselves rejected, since an unauthenticated
        // caller still costs work before being turned away
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post(WEBHOOK_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }

        // when & then
        mockMvc.perform(post(WEBHOOK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests());
    }
}
