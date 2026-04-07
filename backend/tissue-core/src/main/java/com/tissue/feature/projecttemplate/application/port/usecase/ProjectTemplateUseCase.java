package com.tissue.feature.projecttemplate.application.port.usecase;

import com.tissue.feature.projecttemplate.application.dto.request.CreateTemplateFromProjectCommand;
import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateResponse;

public interface ProjectTemplateUseCase {

    ProjectTemplateResponse createFromProject(CreateTemplateFromProjectCommand cmd, Long actorMemberId);

    void delete(String workspaceKey, Long templateId, Long actorMemberId);
}
