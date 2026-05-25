package com.tissue.feature.issue.application.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.exception.InvalidCursorException;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.io.IOException;
import java.util.Base64;
import org.jspecify.annotations.Nullable;

/**
 * Opaque pagination cursor for the keyset-based FTS endpoint. Encodes the
 * (priority, id) of the last row in the previous page, so the next request
 * resumes with {@code (priority > p) OR (priority = p AND id < lastId)}.
 *
 * <p>The token is Base64(JSON). Clients must treat it as opaque — the encoding
 * may change without breaking the API contract as long as the server can decode
 * any token it previously issued.
 *
 * <p>Bound to the fixed sort {@code priority ASC, id DESC} used by the cursor
 * endpoint. If the sort ever becomes configurable, the cursor schema needs to
 * be extended (or versioned) accordingly.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        model = "claude-opus-4-7-max",
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Used the cursor implementation (before using keyset).",
        reviewedBy = "kiseki1011")
public record IssueSearchCursor(IssuePriority priority, Long id) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String encode() {
        try {
            byte[] json = MAPPER.writeValueAsBytes(this);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    public static @Nullable IssueSearchCursor decode(@Nullable String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(token);
            return MAPPER.readValue(json, IssueSearchCursor.class);
        } catch (IllegalArgumentException | IOException e) {
            throw new InvalidCursorException(token, e);
        }
    }
}
