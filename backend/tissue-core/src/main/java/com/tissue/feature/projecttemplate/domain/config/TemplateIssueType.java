package com.tissue.feature.projecttemplate.domain.config;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import java.util.List;

public record TemplateIssueType(
        String name,
        String description,
        ColorType color,
        IconType icon,
        IssueHierarchy hierarchy,
        String workflowTempId,
        List<TemplateIssueField> fields) {

    public static TemplateIssueType from(IssueType it) {
        List<TemplateIssueField> fields = it.getFields().stream()
                .map(f -> new TemplateIssueField(
                        f.getName(),
                        f.getDescription(),
                        f.getIssueFieldType(),
                        f.isRequired(),
                        f.getPosition(),
                        f.getOptions().stream().map(FieldOption::getName).toList()))
                .toList();

        return new TemplateIssueType(
                it.getName(),
                it.getDescription(),
                it.getColor(),
                it.getIcon(),
                it.getIssueHierarchy(),
                it.getWorkflow().getId().toString(),
                fields);
    }
}
