package com.tissue.feature.issuetype.application.service;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.application.port.usecase.IssueTypeUseCase;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.application.service.validator.IssueTypeValidator;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
import com.tissue.support.util.Patchers;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueTypeService implements IssueTypeUseCase {

    private final WorkflowFinder workflowFinder;
    private final IssueTypeFinder issueTypeFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueTypeRepository issueTypeRepository;
    private final IssueTypeValidator issueTypeValidator;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public IssueTypeResponse create(ProjectIdentifier pid, CreateIssueTypeCommand cmd, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        Workflow workflow = workflowFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), cmd.workflowId());

        projectAuthorizationService.requireProjectManager(actor);

        issueTypeValidator.ensureUniqueLabel(workflow.getProject(), cmd.name());

        IssueType issueType = IssueType.create(
                workflow.getProject(),
                cmd.name(),
                cmd.description(),
                cmd.color(),
                cmd.icon(),
                cmd.issueHierarchy(),
                workflow);

        IssueType savedType = issueTypeRepository.save(issueType);

        return IssueTypeResponse.from(savedType);
    }

    @Override
    public void rename(String workspaceKey, Long issueTypeId, Name name, Long actorMemberId) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(workspaceKey, issueTypeId);
        String projectKey = issueType.getProject().getKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        if (isNameUnchanged(issueType, name)) {
            return;
        }

        issueTypeValidator.ensureUniqueLabel(issueType.getProject(), name);
        issueType.rename(name);
    }

    @Override
    public void update(String workspaceKey, Long issueTypeId, PatchIssueTypeCommand cmd, Long actorMemberId) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(workspaceKey, issueTypeId);
        String projectKey = issueType.getProject().getKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Patchers.apply(cmd.description(), issueType::updateDescription);
        Patchers.apply(cmd.color(), issueType::updateColor);
        Patchers.apply(cmd.icon(), issueType::updateIcon);
    }

    @Override
    public void delete(String workspaceKey, Long issueTypeId, Long actorMemberId) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(workspaceKey, issueTypeId);
        String projectKey = issueType.getProject().getKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        issueTypeValidator.ensureDeletable(issueType);

        issueTypeRepository.delete(issueType);
    }

    @Override
    public void reorderFields(String workspaceKey, Long issueTypeId, List<Long> orderedIds, Long actorMemberId) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(workspaceKey, issueTypeId);
        String projectKey = issueType.getProject().getKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        issueType.reorderFields(orderedIds);
    }

    private boolean isNameUnchanged(IssueType it, Name newName) {
        return Objects.equals(it.getName(), newName.toString());
    }
}
