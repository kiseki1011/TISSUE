package com.tissue.issuetype.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.global.vo.Name;
import com.tissue.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.DeleteIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.RenameIssueTypeCommand;
import com.tissue.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.issuetype.application.port.in.IssueTypeUseCase;
import com.tissue.issuetype.application.port.out.IssueTypeCommandRepository;
import com.tissue.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.issuetype.application.service.validator.IssueTypeValidator;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.workflow.application.service.finder.WorkflowFinder;
import com.tissue.workflow.domain.Workflow;
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
    private final ProjectAuthorizationService projectAuthService;

    @Override
    public IssueTypeResponse create(CreateIssueTypeCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Workflow workflow = workflowFinder.getWithProjectBy(
                actorContext.workspaceKey(), actorContext.projectKey(), cmd.workflowId());

        issueTypeValidator.ensureUniqueLabel(workflow.getProject(), cmd.name());

        IssueType issueType = IssueType.create(
                workflow.getProject(), cmd.name(), cmd.description(), cmd.color(), cmd.issueHierarchy(), workflow);

        IssueType savedType = issueTypeCommandRepository.save(issueType);

        return IssueTypeResponse.from(savedType);
    }

    @Override
    public void rename(RenameIssueTypeCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        IssueType issueType = issueTypeFinder.getWithProjectBy(
                actorContext.workspaceKey(), actorContext.projectKey(), cmd.issueTypeId());

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueType);

        if (labelUnchanged(issueType, cmd.name())) {
            return;
        }

        issueTypeValidator.ensureUniqueLabel(issueType.getProject(), cmd.name());
        issueType.rename(cmd.name());
    }

    @Override
    public void update(PatchIssueTypeCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        IssueType issueType = issueTypeFinder.getWithProjectBy(
                actorContext.workspaceKey(), actorContext.projectKey(), cmd.issueTypeId());

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueType);

        Patchers.apply(cmd.description(), issueType::updateDescription);
        Patchers.apply(cmd.color(), issueType::updateColor);
    }

    @Override
    public void delete(DeleteIssueTypeCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        IssueType issueType = issueTypeFinder.getWithProjectBy(
                actorContext.workspaceKey(), actorContext.projectKey(), cmd.issueTypeId());

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueType);

        // TODO: consider IssueType migration feature(make it in IssueConfigUseCase in issue package)
        //  current policy: cant delete if there is a issue that uses this IssueType
        issueTypeValidator.ensureDeletable(issueType);

        issueType.softDelete();
    }

    private boolean labelUnchanged(IssueType it, Name newName) {
        return Objects.equals(it.getName(), newName.toString());
    }
}
