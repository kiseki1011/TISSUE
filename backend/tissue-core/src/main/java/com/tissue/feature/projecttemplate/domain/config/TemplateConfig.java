package com.tissue.feature.projecttemplate.domain.config;

import java.util.List;

public record TemplateConfig(List<TemplateWorkflow> workflows, List<TemplateIssueType> issueTypes) {}
