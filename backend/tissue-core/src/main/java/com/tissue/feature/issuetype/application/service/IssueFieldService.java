package com.tissue.feature.issuetype.application.service;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.feature.issuetype.application.dto.response.ReorderedOptionsResponse;
import com.tissue.feature.issuetype.application.port.repository.EnumFieldOptionRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.application.port.usecase.IssueFieldUseCase;
import com.tissue.feature.issuetype.application.service.finder.IssueFieldFinder;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.application.service.validator.IssueFieldValidator;
import com.tissue.feature.issuetype.domain.EnumFieldOption;
import com.tissue.feature.issuetype.domain.EnumFieldOptions;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.issuetype.domain.policy.FieldDefintionPolicy;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
import com.tissue.support.util.Patchers;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
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
    private final EnumFieldOptionRepository enumfieldOptionRepository;
    private final IssueFieldValidator issueFieldValidator;
    private final FieldDefintionPolicy fieldDefintionPolicy;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final EntityManager entityManager;

    @Override
    public IssueFieldResponse create(
            ProjectIdentifier projectIdentifier, Long issueTypeId, CreateIssueFieldCommand cmd, Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        IssueType issueType = issueTypeFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), issueTypeId);

        projectAuthorizationService.requireProjectManager(actor);

        issueFieldValidator.ensureUniqueLabel(issueType, cmd.name());

        IssueField issueField =
                IssueField.create(cmd.name(), cmd.description(), cmd.issueFieldType(), cmd.required(), issueType);

        IssueField savedField = issueFieldRepository.save(issueField);

        if (savedField.getIssueFieldType() == IssueFieldType.ENUM) {
            fieldDefintionPolicy.ensureOptionsWithinLimit(cmd.initialOptions());
            saveInitialEnumOptions(savedField, cmd.initialOptions());
        }

        return IssueFieldResponse.from(savedField, issueType);
    }

    @Override
    public void rename(
            ProjectIdentifier projectIdentifier, Long issueTypeId, Long issueFieldId, Name name, Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), issueTypeId, issueFieldId);

        projectAuthorizationService.requireProjectManager(actor);

        if (labelUnchanged(issueField.getName(), name.toString())) {
            return;
        }

        issueFieldValidator.ensureUniqueLabel(issueField.getIssueType(), name);
        issueField.rename(name);
    }

    @Override
    public void update(
            ProjectIdentifier projectIdentifier,
            Long issueTypeId,
            Long issueFieldId,
            PatchIssueFieldCommand cmd,
            Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), issueTypeId, issueFieldId);

        projectAuthorizationService.requireProjectManager(actor);

        Patchers.apply(cmd.description(), issueField::updateDescription);
        Patchers.apply(cmd.required(), issueField::setRequired);
    }

    @Override
    public void delete(ProjectIdentifier projectIdentifier, Long issueTypeId, Long issueFieldId, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), issueTypeId, issueFieldId);

        projectAuthorizationService.requireProjectManager(actor);
        issueFieldValidator.ensureDeletable(issueField);

        issueFieldRepository.delete(issueField);
    }

    @Override
    public IssueFieldResponse addOption(
            ProjectIdentifier projectIdentifier, Long issueTypeId, Long issueFieldId, Name name, Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), issueTypeId, issueFieldId);

        projectAuthorizationService.requireProjectManager(actor);
        issueFieldValidator.ensureUniqueOptionLabel(issueField, name);

        int nextPosition = issueFieldFinder.countOptions(issueField);
        fieldDefintionPolicy.ensureCanAddOption(nextPosition);

        EnumFieldOption option = EnumFieldOption.create(issueField, name, nextPosition);
        enumfieldOptionRepository.save(option);

        return IssueFieldResponse.from(issueField, issueField.getIssueType());
    }

    @Override
    public void renameOption(
            ProjectIdentifier projectIdentifier,
            Long issueTypeId,
            Long issueFieldId,
            Long optionId,
            Name name,
            Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        EnumFieldOption option = issueFieldFinder.getWithHierarchyBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), issueTypeId, issueFieldId, optionId);

        projectAuthorizationService.requireProjectManager(actor);

        if (labelUnchanged(option.getName(), name.toString())) {
            return;
        }

        issueFieldValidator.ensureUniqueOptionLabel(option.getIssueField(), name);

        option.rename(name);
    }

    @Override
    public ReorderedOptionsResponse reorderOptions(
            ProjectIdentifier projectIdentifier,
            Long issueTypeId,
            Long issueFieldId,
            List<Long> targetOrderedIds,
            Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), issueTypeId, issueFieldId);

        projectAuthorizationService.requireProjectManager(actor);

        EnumFieldOptions options =
                EnumFieldOptions.fromCurrentOptions(issueField, issueFieldFinder.getAllOptions(issueField));

        options.ensureExactActiveIds(targetOrderedIds);
        options.bumpPositions();

        entityManager.flush();

        options.reorderTo(targetOrderedIds);

        return ReorderedOptionsResponse.from(issueField.getId(), options.getSortedOptions());
    }

    @Override
    public void deleteOption(
            ProjectIdentifier projectIdentifier,
            Long issueTypeId,
            Long issueFieldId,
            Long optionId,
            Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        EnumFieldOption option = issueFieldFinder.getWithHierarchyBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), issueTypeId, issueFieldId, optionId);

        projectAuthorizationService.requireProjectManager(actor);
        issueFieldValidator.ensureOptionDeletable(option);

        enumfieldOptionRepository.delete(option);
    }

    private boolean labelUnchanged(String currentName, String newName) {
        return Objects.equals(currentName, newName);
    }

    private void saveInitialEnumOptions(IssueField field, List<Name> names) {
        int pos = 0;
        List<EnumFieldOption> options = new ArrayList<>(names.size());
        for (Name name : names) {
            options.add(EnumFieldOption.create(field, name, pos++));
        }
        enumfieldOptionRepository.saveAll(options);
    }
}
