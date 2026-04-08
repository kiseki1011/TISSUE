package com.tissue.feature.projecttemplate.application.service;

import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.projecttemplate.application.dto.request.CreateTemplateFromProjectCommand;
import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateResponse;
import com.tissue.feature.projecttemplate.application.port.repository.ProjectTemplateRepository;
import com.tissue.feature.projecttemplate.application.port.usecase.ProjectTemplateUseCase;
import com.tissue.feature.projecttemplate.domain.ProjectTemplate;
import com.tissue.feature.projecttemplate.domain.config.TemplateConfig;
import com.tissue.feature.projecttemplate.domain.config.TemplateIssueType;
import com.tissue.feature.projecttemplate.domain.config.TemplateWorkflow;
import com.tissue.feature.projecttemplate.domain.exception.ProjectTemplateNotFoundException;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectTemplateService implements ProjectTemplateUseCase {

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final WorkflowRepository workflowRepository;
    private final ProjectTemplateRepository projectTemplateRepository;

    @Override
    public ProjectTemplateResponse createFromProject(CreateTemplateFromProjectCommand cmd, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(cmd.workspaceKey(), cmd.projectKey(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Project project = projectFinder.getBy(cmd.workspaceKey(), cmd.projectKey());

        List<Workflow> workflows = workflowRepository.findAllByProjectOrderByLabel(project);
        List<TemplateWorkflow> templateWorkflows =
                workflows.stream().map(TemplateWorkflow::from).toList();

        List<IssueType> issueTypes = project.getIssueTypes();
        List<TemplateIssueType> templateIssueTypes =
                issueTypes.stream().map(TemplateIssueType::from).toList();

        TemplateConfig config = new TemplateConfig(templateWorkflows, templateIssueTypes);

        ProjectTemplate template =
                ProjectTemplate.create(project.getWorkspace(), cmd.name(), cmd.description(), config);

        projectTemplateRepository.save(template);

        return ProjectTemplateResponse.from(template);
    }

    @Override
    public void delete(String workspaceKey, Long templateId, Long actorMemberId) {
        ProjectTemplate template = projectTemplateRepository
                .findByIdAndWorkspaceKey(templateId, workspaceKey)
                .orElseThrow(() -> new ProjectTemplateNotFoundException(templateId));

        projectTemplateRepository.delete(template);
    }
}
