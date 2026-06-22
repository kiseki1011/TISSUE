package com.tissue.feature.issue.domain.service.handler;

import com.tissue.feature.issue.domain.policy.IssuePolicy;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

/**
 * Single-line text, length-capped by {@link IssuePolicy#ensureShortTextLength}. Unlike TEXT (multi-line,
 * Markdown-rendered) this is meant for short labels such as a version or environment name.
 */
@Component
@RequiredArgsConstructor
@LLMGenerated(llmInvolvement = LLMInvolvement.VIBE_CODED, model = "claude-opus-4-8")
public class ShortTextFieldHandler implements FieldTypeHandler {

    private final IssuePolicy policy;

    @Qualifier("domainConversionService")
    private final ConversionService cs;

    @Override
    public IssueFieldType type() {
        return IssueFieldType.SHORT_TEXT;
    }

    @Override
    public @Nullable Object parse(IssueField field, @Nullable Object raw) {
        String value = convert(cs, raw, String.class, field);
        if (value == null) {
            return null;
        }
        policy.ensureShortTextLength(value);
        return value;
    }

    @Override
    public @Nullable Object toJsonValue(@Nullable Object domainValue) {
        return domainValue;
    }

    @Override
    public @Nullable Object fromJsonValue(@Nullable Object jsonValue) {
        return jsonValue;
    }
}
