package com.tissue.feature.issuetype.application.service;

import com.tissue.feature.issue.domain.policy.IssuePolicy;
import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.application.port.usecase.IssueFieldUseCase;
import com.tissue.feature.issuetype.application.service.finder.IssueFieldFinder;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.application.service.validator.IssueFieldValidator;
import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.vo.Name;
import com.tissue.support.util.Patchers;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueFieldService implements IssueFieldUseCase {

    private final IssueTypeFinder issueTypeFinder;
    private final IssueFieldFinder issueFieldFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueFieldRepository issueFieldRepository;
    private final IssueFieldValidator issueFieldValidator;
    private final IssuePolicy issuePolicy;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public IssueFieldResponse addField(
            String workspaceKey, Long issueTypeId, CreateIssueFieldCommand cmd, Long actorMemberId) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(workspaceKey, issueTypeId);
        String projectKey = issueType.getProject().getKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        issueFieldValidator.ensureUniqueLabel(issueType, cmd.name());

        IssueField issueField =
                issueType.addField(cmd.name(), cmd.description(), cmd.issueFieldType(), cmd.required(), cmd.position());

        if (issueField.getIssueFieldType().canHaveOptions()) {
            issuePolicy.ensureCanAddOption(cmd.initialOptions().size());
            for (Name optionName : cmd.initialOptions()) {
                issueField.addOption(optionName);
            }
        }

        IssueField savedField = issueFieldRepository.save(issueField);

        return IssueFieldResponse.from(savedField, issueType);
    }

    @Override
    public void rename(String workspaceKey, Long issueFieldId, Name name, Long actorMemberId) {
        IssueField issueField = issueFieldFinder.getWithProjectAndIssueType(workspaceKey, issueFieldId);
        String projectKey = issueField.getIssueType().getProject().getKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        if (labelUnchanged(issueField.getName(), name.toString())) {
            return;
        }

        issueFieldValidator.ensureUniqueLabel(issueField.getIssueType(), name);
        issueField.rename(name);
    }

    @Override
    public void update(String workspaceKey, Long issueFieldId, PatchIssueFieldCommand cmd, Long actorMemberId) {
        IssueField issueField = issueFieldFinder.getWithProjectAndIssueType(workspaceKey, issueFieldId);
        String projectKey = issueField.getIssueType().getProject().getKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Patchers.apply(cmd.description(), issueField::updateDescription);
        Patchers.apply(cmd.required(), issueField::setRequired);
    }

    @Override
    public void delete(String workspaceKey, Long issueFieldId, Long actorMemberId) {
        IssueField issueField = issueFieldFinder.getWithProjectAndIssueType(workspaceKey, issueFieldId);
        String projectKey = issueField.getIssueType().getProject().getKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);
        issueFieldValidator.ensureDeletable(issueField);

        issueFieldRepository.delete(issueField);
    }

    @Override
    public IssueFieldResponse addOption(String workspaceKey, Long issueFieldId, Name name, Long actorMemberId) {
        IssueField issueField = issueFieldFinder.getWithProjectAndIssueType(workspaceKey, issueFieldId);
        String projectKey = issueField.getIssueType().getProject().getKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);
        issueFieldValidator.ensureUniqueOptionLabel(issueField, name);

        issuePolicy.ensureCanAddOption(issueField.getOptions().size());

        issueField.addOption(name);

        return IssueFieldResponse.from(issueField, issueField.getIssueType());
    }

    @Override
    public void renameOption(String workspaceKey, Long issueFieldId, Long optionId, Name name, Long actorMemberId) {
        FieldOption option = issueFieldFinder.getWithHierarchy(workspaceKey, issueFieldId, optionId);
        String projectKey = option.getIssueField().getIssueType().getProject().getKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        if (labelUnchanged(option.getName(), name.toString())) {
            return;
        }

        issueFieldValidator.ensureUniqueOptionLabel(option.getIssueField(), name);

        option.rename(name);
    }

    @Override
    public void deleteOption(String workspaceKey, Long issueFieldId, Long optionId, Long actorMemberId) {
        FieldOption option = issueFieldFinder.getWithHierarchy(workspaceKey, issueFieldId, optionId);
        String projectKey = option.getIssueField().getIssueType().getProject().getKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);
        issueFieldValidator.ensureOptionDeletable(option);

        option.getIssueField().removeOption(option);
    }

    private boolean labelUnchanged(String currentName, String newName) {
        return Objects.equals(currentName, newName);
    }
}
