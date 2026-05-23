package com.tissue.feature.issuetype.application.dto.response;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import java.util.List;

public record IssueTypeDetail(
        Long id,
        String name,
        String description,
        IconType icon,
        ColorType color,
        IssueHierarchy hierarchy,
        Long workflowId,
        String workflowName,
        boolean systemProvided,
        List<IssueFieldDetail> fields) {

    public static IssueTypeDetail of(IssueType issueType, List<IssueField> fields) {
        List<IssueFieldDetail> fieldDetails =
                fields.stream().map(IssueFieldDetail::from).toList();

        return new IssueTypeDetail(
                issueType.getId(),
                issueType.getName(),
                issueType.getDescription(),
                issueType.getIcon(),
                issueType.getColor(),
                issueType.getIssueHierarchy(),
                issueType.getWorkflow().getId(),
                issueType.getWorkflow().getName(),
                issueType.isSystemProvided(),
                fieldDetails);
    }
}
