package com.tissue.feature.issuetype.application.service;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.feature.issuetype.application.port.in.IssueTypeUseCase;
import com.tissue.feature.issuetype.application.port.out.IssueTypeCommandRepository;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.application.service.validator.IssueTypeValidator;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
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
    private final IssueTypeCommandRepository issueTypeCommandRepository;
    private final IssueTypeValidator issueTypeValidator;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public IssueTypeResponse create(
            String projectKey, CreateIssueTypeCommand cmd, WorkspaceMemberContext actorContext) {
        Workflow workflow = workflowFinder.getWithProjectBy(actorContext.workspaceKey(), projectKey, cmd.workflowId());

        projectAuthorizationService.requireProjectEditPermission(actorContext, workflow.getProject());

        issueTypeValidator.ensureUniqueLabel(workflow.getProject(), cmd.name());

        IssueType issueType = IssueType.create(
                workflow.getProject(), cmd.name(), cmd.description(), cmd.color(), cmd.issueHierarchy(), workflow);

        IssueType savedType = issueTypeCommandRepository.save(issueType);

        return IssueTypeResponse.from(savedType);
    }

    @Override
    public void rename(String projectKey, Long issueTypeId, Name name, WorkspaceMemberContext actorContext) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(actorContext.workspaceKey(), projectKey, issueTypeId);

        projectAuthorizationService.requireProjectEditPermission(actorContext, issueType.getProject());

        if (labelUnchanged(issueType, name)) {
            return;
        }

        issueTypeValidator.ensureUniqueLabel(issueType.getProject(), name);
        issueType.rename(name);
    }

    @Override
    public void update(
            String projectKey, Long issueTypeId, PatchIssueTypeCommand cmd, WorkspaceMemberContext actorContext) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(actorContext.workspaceKey(), projectKey, issueTypeId);

        projectAuthorizationService.requireProjectEditPermission(actorContext, issueType.getProject());

        Patchers.apply(cmd.description(), issueType::updateDescription);
        Patchers.apply(cmd.color(), issueType::updateColor);
    }

    @Override
    public void delete(String projectKey, Long issueTypeId, WorkspaceMemberContext actorContext) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(actorContext.workspaceKey(), projectKey, issueTypeId);

        projectAuthorizationService.requireProjectEditPermission(actorContext, issueType.getProject());

        // TODO: consider IssueType migration feature(make it in IssueConfigUseCase in issue package)
        //  current policy: cant delete if there is a issue that uses this IssueType
        issueTypeValidator.ensureDeletable(issueType);

        issueTypeCommandRepository.delete(issueType);
    }

    private boolean labelUnchanged(IssueType it, Name newName) {
        return Objects.equals(it.getName(), newName.toString());
    }
}
