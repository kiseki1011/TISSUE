package com.tissue.feature.issuetype.application.service;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeCommandRepository;
import com.tissue.feature.issuetype.application.port.usecase.IssueTypeUseCase;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.application.service.validator.IssueTypeValidator;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
import com.tissue.support.util.Patchers;
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
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final IssueTypeCommandRepository issueTypeCommandRepository;
    private final IssueTypeValidator issueTypeValidator;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public IssueTypeResponse create(ProjectIdentifier projectIdentifier, CreateIssueTypeCommand cmd, Long memberId) {
        Workflow workflow = workflowFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), cmd.workflowId());

        WorkspaceMember actor = workspaceMemberFinder.getBy(projectIdentifier.workspaceKey(), memberId);
        projectAuthorizationService.requireProjectEditPermission(actor, workflow.getProject());

        issueTypeValidator.ensureUniqueLabel(workflow.getProject(), cmd.name());

        IssueType issueType = IssueType.create(
                workflow.getProject(), cmd.name(), cmd.description(), cmd.color(), cmd.issueHierarchy(), workflow);

        IssueType savedType = issueTypeCommandRepository.save(issueType);

        return IssueTypeResponse.from(savedType);
    }

    @Override
    public void rename(ProjectIdentifier projectIdentifier, Long issueTypeId, Name name, Long memberId) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), issueTypeId);

        WorkspaceMember actor = workspaceMemberFinder.getBy(projectIdentifier.workspaceKey(), memberId);
        projectAuthorizationService.requireProjectEditPermission(actor, issueType.getProject());

        if (labelUnchanged(issueType, name)) {
            return;
        }

        issueTypeValidator.ensureUniqueLabel(issueType.getProject(), name);
        issueType.rename(name);
    }

    @Override
    public void update(
            ProjectIdentifier projectIdentifier, Long issueTypeId, PatchIssueTypeCommand cmd, Long memberId) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), issueTypeId);

        WorkspaceMember actor = workspaceMemberFinder.getBy(projectIdentifier.workspaceKey(), memberId);
        projectAuthorizationService.requireProjectEditPermission(actor, issueType.getProject());

        Patchers.apply(cmd.description(), issueType::updateDescription);
        Patchers.apply(cmd.color(), issueType::updateColor);
    }

    @Override
    public void delete(ProjectIdentifier projectIdentifier, Long issueTypeId, Long memberId) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), issueTypeId);

        WorkspaceMember actor = workspaceMemberFinder.getBy(projectIdentifier.workspaceKey(), memberId);
        projectAuthorizationService.requireProjectEditPermission(actor, issueType.getProject());

        // TODO: consider IssueType migration feature(make it in IssueConfigUseCase in issue package)
        //  current policy: cant delete if there is a issue that uses this IssueType
        issueTypeValidator.ensureDeletable(issueType);

        issueTypeCommandRepository.delete(issueType);
    }

    private boolean labelUnchanged(IssueType it, Name newName) {
        return Objects.equals(it.getName(), newName.toString());
    }
}
