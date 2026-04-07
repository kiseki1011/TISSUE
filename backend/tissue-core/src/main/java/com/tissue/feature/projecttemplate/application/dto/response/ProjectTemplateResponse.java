package com.tissue.feature.projecttemplate.application.dto.response;

import com.tissue.feature.projecttemplate.domain.ProjectTemplate;

public record ProjectTemplateResponse(Long id) {
    public static ProjectTemplateResponse from(ProjectTemplate template) {
        return new ProjectTemplateResponse(template.getId());
    }
}
