package com.tissue.issuetype.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.global.vo.Name;
import com.tissue.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.issuetype.application.dto.response.ReorderedOptionsResponse;
import com.tissue.issuetype.application.port.in.IssueFieldUseCase;
import com.tissue.issuetype.application.port.out.EnumFieldOptionCommandRepository;
import com.tissue.issuetype.application.port.out.IssueFieldCommandRepository;
import com.tissue.issuetype.application.service.finder.IssueFieldFinder;
import com.tissue.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.issuetype.application.service.validator.IssueFieldValidator;
import com.tissue.issuetype.domain.EnumFieldOption;
import com.tissue.issuetype.domain.EnumFieldOptions;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import com.tissue.issuetype.domain.policy.FieldDefintionPolicy;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
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

    private final ProjectFinder projectFinder;
    private final IssueTypeFinder issueTypeFinder;
    private final IssueFieldFinder issueFieldFinder;

    private final IssueFieldCommandRepository issueFieldCommandRepo;
    private final EnumFieldOptionCommandRepository fieldOptionCommandRepo;

    private final IssueFieldValidator issueFieldValidator;
    private final FieldDefintionPolicy fieldDefintionPolicy;

    private final ProjectAuthorizationService projectAuthService;
    private final EntityManager entityManager;

    @Override
    public IssueFieldResponse create(Long issueTypeId, CreateIssueFieldCommand cmd, ProjectMemberContext actorContext) {
        IssueType issueType =
                issueTypeFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), issueTypeId);

        // TODO: requireProjectEditPermission

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
    public void rename(Long issueTypeId, Long issueFieldId, Name name, ProjectMemberContext actorContext) {
        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                actorContext.workspaceKey(), actorContext.projectKey(), issueTypeId, issueFieldId);

        // TODO: requireProjectEditPermission

        if (labelUnchanged(issueField.getName(), name.toString())) {
            return;
        }

        issueFieldValidator.ensureUniqueLabel(issueField.getIssueType(), name);
        issueField.rename(name);
    }

    @Override
    public void update(
            Long issueTypeId, Long issueFieldId, PatchIssueFieldCommand cmd, ProjectMemberContext actorContext) {
        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                actorContext.workspaceKey(), actorContext.projectKey(), issueTypeId, issueFieldId);

        // TODO: requireProjectEditPermission

        Patchers.apply(cmd.description(), issueField::updateDescription);
        Patchers.apply(cmd.required(), issueField::setRequired);
    }

    @Override
    public void delete(Long issueTypeId, Long issueFieldId, ProjectMemberContext actorContext) {
        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                actorContext.workspaceKey(), actorContext.projectKey(), issueTypeId, issueFieldId);

        // TODO: requireProjectEditPermission
        issueFieldValidator.ensureDeletable(issueField);

        issueFieldCommandRepo.delete(issueField);
    }

    @Override
    public IssueFieldResponse addOption(
            Long issueTypeId, Long issueFieldId, Name name, ProjectMemberContext actorContext) {
        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                actorContext.workspaceKey(), actorContext.projectKey(), issueTypeId, issueFieldId);

        // TODO: requireProjectEditPermission
        issueFieldValidator.ensureUniqueOptionLabel(issueField, name);

        int nextPosition = issueFieldFinder.countOptions(issueField);
        fieldDefintionPolicy.ensureCanAddOption(nextPosition);

        EnumFieldOption option = EnumFieldOption.create(issueField, name, nextPosition);
        fieldOptionCommandRepo.save(option);

        return IssueFieldResponse.from(issueField, issueField.getIssueType());
    }

    @Override
    public void renameOption(
            Long issueTypeId, Long issueFieldId, Long optionId, Name name, ProjectMemberContext actorContext) {
        String workspaceKey = actorContext.workspaceKey();
        String projectKey = actorContext.projectKey();

        EnumFieldOption option =
                issueFieldFinder.getWithHierarchyBy(workspaceKey, projectKey, issueTypeId, issueFieldId, optionId);

        // TODO: requireProjectEditPermission

        if (labelUnchanged(option.getName(), name.toString())) {
            return;
        }

        issueFieldValidator.ensureUniqueOptionLabel(option.getIssueField(), name);

        option.rename(name);
    }

    @Override
    public ReorderedOptionsResponse reorderOptions(
            Long issueTypeId, Long issueFieldId, List<Long> targetOrderedIds, ProjectMemberContext actorContext) {
        IssueField issueField = issueFieldFinder.getWithProjectAndIssueTypeBy(
                actorContext.workspaceKey(), actorContext.projectKey(), issueTypeId, issueFieldId);

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueField.getIssueType());

        EnumFieldOptions options =
                EnumFieldOptions.fromCurrentOptions(issueField, issueFieldFinder.getAllOptions(issueField));

        options.ensureExactActiveIds(targetOrderedIds);
        options.bumpPositions();

        entityManager.flush();

        options.reorderTo(targetOrderedIds);

        return ReorderedOptionsResponse.from(issueField.getId(), options.getSortedOptions());
    }

    @Override
    public void deleteOption(Long issueTypeId, Long issueFieldId, Long optionId, ProjectMemberContext actorContext) {
        String workspaceKey = actorContext.workspaceKey();
        String projectKey = actorContext.projectKey();

        EnumFieldOption option =
                issueFieldFinder.getWithHierarchyBy(workspaceKey, projectKey, issueTypeId, issueFieldId, optionId);

        projectAuthService.requireIssueTypeEditPermission(
                actorContext, option.getIssueField().getIssueType());
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
