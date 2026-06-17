package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.support.IntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test the generated-client(openapi-generator) contract for issue search.
 * {@code @ParameterObject} must keep exploding {@code IssueSearchRequest} into individual
 * query params (?keyword=...), not collapse it into a single {@code request} object param
 * (which made generated clients send {@code ?request={json}} and cause a problem).
 * <a href="https://github.com/kiseki1011/TISSUE/pull/519">See PR #519</a>
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        model = "claude-opus-4-8",
        evaluation = Evaluation.NOT_REVIEWED)
@AutoConfigureMockMvc
class IssueSearchOpenApiContractIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("success: search fields are flat query params, not a single 'request' object")
    void searchFieldsAreFlatQueryParams() throws Exception {
        String body =
                mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();

        JsonNode params = objectMapper
                .readTree(body)
                .path("paths")
                .path("/api/v1/issues:search")
                .path("get")
                .path("parameters");

        List<String> names = new ArrayList<>();
        params.forEach(p -> names.add(p.path("name").asText()));

        assertThat(names).contains("keyword", "priorities", "page", "size");
        assertThat(names).doesNotContain("request");
    }
}
