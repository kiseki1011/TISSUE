package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.issuetype.application.dto.response.FieldOptionDetail;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CustomFieldValueInfo(
        Long fieldId,
        String fieldLabel,
        IssueFieldType issueFieldType,
        boolean required,
        @Nullable Object value,
        List<FieldOptionDetail> options) {}
