package com.tissue.feature.projecttemplate.application.service;

import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateDetail;
import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateSummary;
import com.tissue.feature.projecttemplate.application.port.repository.ProjectTemplateRepository;
import com.tissue.feature.projecttemplate.application.port.usecase.ProjectTemplateQueryUseCase;
import com.tissue.feature.projecttemplate.domain.ProjectTemplate;
import com.tissue.feature.projecttemplate.domain.exception.ProjectTemplateNotFoundException;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectTemplateQueryService implements ProjectTemplateQueryUseCase {

    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final ProjectTemplateRepository projectTemplateRepository;

    @Override
    public Page<ProjectTemplateSummary> getWorkspaceTemplates(
            String workspaceKey, Pageable pageable, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        return projectTemplateRepository
                .pageByWorkspaceKey(workspaceKey, pageable)
                .map(ProjectTemplateSummary::from);
    }

    @Override
    public ProjectTemplateDetail getProjectTemplateDetail(String workspaceKey, Long templateId, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        ProjectTemplate template = projectTemplateRepository
                .findByIdAndWorkspaceKey(templateId, workspaceKey)
                .orElseThrow(() -> new ProjectTemplateNotFoundException(templateId));

        return ProjectTemplateDetail.from(template);
    }
}
