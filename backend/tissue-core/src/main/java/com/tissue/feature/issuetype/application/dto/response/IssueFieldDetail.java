package com.tissue.feature.issuetype.application.dto.response;

import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import java.util.List;

public record IssueFieldDetail(
        Long id,
        String name,
        String description,
        IssueFieldType type,
        boolean required,
        int position,
        List<FieldOptionDetail> options) {

    public static IssueFieldDetail from(IssueField field) {
        List<FieldOptionDetail> options =
                field.getOptions().stream().map(FieldOptionDetail::from).toList();

        return new IssueFieldDetail(
                field.getId(),
                field.getName(),
                field.getDescription(),
                field.getIssueFieldType(),
                field.isRequired(),
                field.getPosition(),
                options);
    }
}
