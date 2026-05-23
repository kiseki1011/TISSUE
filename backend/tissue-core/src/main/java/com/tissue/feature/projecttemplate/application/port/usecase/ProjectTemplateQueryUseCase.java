package com.tissue.feature.projecttemplate.application.port.usecase;

import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateDetail;
import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectTemplateQueryUseCase {

    Page<ProjectTemplateSummary> getWorkspaceTemplates(String workspaceKey, Pageable pageable, Long actorMemberId);

    ProjectTemplateDetail getProjectTemplateDetail(String workspaceKey, Long templateId, Long actorMemberId);
}
