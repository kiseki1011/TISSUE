package com.tissue.feature.projecttemplate.domain.config;

import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import java.util.List;

public record TemplateIssueField(
        String name, String description, IssueFieldType type, boolean required, int position, List<String> options) {}
