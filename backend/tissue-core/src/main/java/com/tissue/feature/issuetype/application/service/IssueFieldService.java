package com.tissue.feature.issuetype.application.service;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.feature.issuetype.application.dto.response.ReorderedOptionsResponse;
import com.tissue.feature.issuetype.application.port.in.IssueFieldUseCase;
import com.tissue.feature.issuetype.application.port.out.EnumFieldOptionCommandRepository;
import com.tissue.feature.issuetype.application.port.out.IssueFieldCommandRepository;
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
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
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

    private final IssueFieldCommandRepository issueFieldCommandRepo;
    private final EnumFieldOptionCommandRepository fieldOptionCommandRepo;

    private final IssueFieldValidator issueFieldValidator;
    private final FieldDefintionPolicy fieldDefintionPolicy;

    private final ProjectAuthorizationService projectAuthorizationService;
    private final EntityManager entityManager;

    @Override
    public IssueFieldResponse create(
            String projectKey, Long issueTypeId, CreateIssueFieldCommand cmd, WorkspaceMemberContext actorContext) {

        IssueType issueType = issueTypeFinder.getWithProjectBy(actorContext.workspaceKey(), projectKey, issueTypeId);

        projectAuthorizationService.requireProjectEditPermission(actorContext, issueType.getProject());

        issueFieldValidator.ensureUniqueLabel(issueType, cmd.name());

        IssueField issueField =
                IssueField.create(cmd.name(), cmd.description(), cmd.issueFieldType(), cmd.required(), issueType);

        IssueField savedField = issueFieldCommandRepo.save(issueField);

        if (savedField.getIssueFieldType() == IssueFieldType.ENUM) {
            fieldDefintionPolicy.ensureOptionsWithinLimit(cmd.initialOptions());
            saveInitialEnumOptions(savedField, cmd.initialOptions());
        }

        return IssueFieldResponse.from(savedField, issueType);
    }

    @Override
    public void rename(
            String projectKey, Long issueTypeId, Long issueFieldId, Name name, WorkspaceMemberContext actorContext) {

        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                actorContext.workspaceKey(), projectKey, issueTypeId, issueFieldId);

        projectAuthorizationService.requireProjectEditPermission(
                actorContext, issueField.getIssueType().getProject());

        if (labelUnchanged(issueField.getName(), name.toString())) {
            return;
        }

        issueFieldValidator.ensureUniqueLabel(issueField.getIssueType(), name);
        issueField.rename(name);
    }

    @Override
    public void update(
            String projectKey,
            Long issueTypeId,
            Long issueFieldId,
            PatchIssueFieldCommand cmd,
            WorkspaceMemberContext actorContext) {

        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                actorContext.workspaceKey(), projectKey, issueTypeId, issueFieldId);

        projectAuthorizationService.requireProjectEditPermission(
                actorContext, issueField.getIssueType().getProject());

        Patchers.apply(cmd.description(), issueField::updateDescription);
        Patchers.apply(cmd.required(), issueField::setRequired);
    }

    @Override
    public void delete(String projectKey, Long issueTypeId, Long issueFieldId, WorkspaceMemberContext actorContext) {
        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                actorContext.workspaceKey(), projectKey, issueTypeId, issueFieldId);

        projectAuthorizationService.requireProjectEditPermission(
                actorContext, issueField.getIssueType().getProject());

        issueFieldValidator.ensureDeletable(issueField);

        issueFieldCommandRepo.delete(issueField);
    }

    @Override
    public IssueFieldResponse addOption(
            String projectKey, Long issueTypeId, Long issueFieldId, Name name, WorkspaceMemberContext actorContext) {

        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                actorContext.workspaceKey(), projectKey, issueTypeId, issueFieldId);

        projectAuthorizationService.requireProjectEditPermission(
                actorContext, issueField.getIssueType().getProject());

        issueFieldValidator.ensureUniqueOptionLabel(issueField, name);

        int nextPosition = issueFieldFinder.countOptions(issueField);
        fieldDefintionPolicy.ensureCanAddOption(nextPosition);

        EnumFieldOption option = EnumFieldOption.create(issueField, name, nextPosition);
        fieldOptionCommandRepo.save(option);

        return IssueFieldResponse.from(issueField, issueField.getIssueType());
    }

    @Override
    public void renameOption(
            String projectKey,
            Long issueTypeId,
            Long issueFieldId,
            Long optionId,
            Name name,
            WorkspaceMemberContext actorContext) {

        EnumFieldOption option = issueFieldFinder.getWithHierarchyBy(
                actorContext.workspaceKey(), projectKey, issueTypeId, issueFieldId, optionId);

        projectAuthorizationService.requireProjectEditPermission(
                actorContext, option.getIssueField().getIssueType().getProject());

        if (labelUnchanged(option.getName(), name.toString())) {
            return;
        }

        issueFieldValidator.ensureUniqueOptionLabel(option.getIssueField(), name);

        option.rename(name);
    }

    @Override
    public ReorderedOptionsResponse reorderOptions(
            String projectKey,
            Long issueTypeId,
            Long issueFieldId,
            List<Long> targetOrderedIds,
            WorkspaceMemberContext actorContext) {

        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                actorContext.workspaceKey(), projectKey, issueTypeId, issueFieldId);

        projectAuthorizationService.requireProjectEditPermission(
                actorContext, issueField.getIssueType().getProject());

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
            String projectKey,
            Long issueTypeId,
            Long issueFieldId,
            Long optionId,
            WorkspaceMemberContext actorContext) {

        EnumFieldOption option = issueFieldFinder.getWithHierarchyBy(
                actorContext.workspaceKey(), projectKey, issueTypeId, issueFieldId, optionId);

        projectAuthorizationService.requireProjectEditPermission(
                actorContext, option.getIssueField().getIssueType().getProject());

        issueFieldValidator.ensureOptionDeletable(option);

        fieldOptionCommandRepo.delete(option);
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
        fieldOptionCommandRepo.saveAll(options);
    }
}
