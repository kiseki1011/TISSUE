package com.tissue.feature.projecttemplate.domain.exception;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class ProjectTemplateNotFoundException extends ResourceNotFoundException {

    public ProjectTemplateNotFoundException(Long templateId) {
        super(ProjectTemplateErrorCode.PROJECT_TEMPLATE_NOT_FOUND);
        addContext("templateId", templateId);
    }
}
