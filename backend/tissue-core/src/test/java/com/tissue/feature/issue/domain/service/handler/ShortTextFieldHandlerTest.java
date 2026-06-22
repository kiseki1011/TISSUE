package com.tissue.feature.issue.domain.service.handler;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.SHORT_TEXT_TOO_LONG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issue.domain.policy.IssuePolicy;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.exception.base.BadRequestException;
import java.math.RoundingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;

class ShortTextFieldHandlerTest {

    // shortTextMaxLength = 50 (last arg)
    private final IssuePolicy policy = new IssuePolicy(10, 0, RoundingMode.HALF_UP, 3, 0, 50, 50);
    private final ShortTextFieldHandler handler =
            new ShortTextFieldHandler(policy, DefaultConversionService.getSharedInstance());
    private final IssueField field = mock(IssueField.class);

    @Test
    @DisplayName("type() is SHORT_TEXT")
    void type() {
        assertThat(handler.type()).isEqualTo(IssueFieldType.SHORT_TEXT);
    }

    @Test
    @DisplayName("parses a value at or under the length limit")
    void parseWithinLimit() {
        assertThat(handler.parse(field, "v1.2.3")).isEqualTo("v1.2.3");
        assertThat(handler.parse(field, "a".repeat(50))).isEqualTo("a".repeat(50));
    }

    @Test
    @DisplayName("rejects a value over the length limit")
    void parseOverLimit() {
        assertThatThrownBy(() -> handler.parse(field, "a".repeat(51)))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(SHORT_TEXT_TOO_LONG);
    }

    @Test
    @DisplayName("null passes through (blank handling lives upstream)")
    void parseNull() {
        assertThat(handler.parse(field, null)).isNull();
    }

    @Test
    @DisplayName("json round-trip is pass-through")
    void jsonRoundTrip() {
        assertThat(handler.toJsonValue("v1")).isEqualTo("v1");
        assertThat(handler.fromJsonValue("v1")).isEqualTo("v1");
    }
}
