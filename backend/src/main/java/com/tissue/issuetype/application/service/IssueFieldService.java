package com.tissue.issuetype.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.application.dto.request.AddOptionCommand;
import com.tissue.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.DeleteIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.DeleteOptionCommand;
import com.tissue.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.RenameIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.RenameOptionCommand;
import com.tissue.issuetype.application.dto.request.ReorderOptionsCommand;
import com.tissue.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.issuetype.application.dto.response.ReorderedOptionsResponse;
import com.tissue.issuetype.application.port.in.IssueFieldUseCase;
import com.tissue.issuetype.application.port.out.EnumFieldOptionCommandRepository;
import com.tissue.issuetype.application.port.out.EnumFieldOptionQueryRepository;
import com.tissue.issuetype.application.port.out.IssueFieldCommandRepository;
import com.tissue.issuetype.application.service.finder.IssueFieldFinder;
import com.tissue.issuetype.application.service.finder.IssueFieldOptionFinder;
import com.tissue.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.issuetype.application.service.validator.IssueTypeValidator;
import com.tissue.issuetype.domain.EnumFieldOption;
import com.tissue.issuetype.domain.EnumFieldOptions;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import com.tissue.issuetype.domain.policy.FieldDefintionPolicy;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
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
    private final IssueFieldOptionFinder fieldOptionFinder;

    private final IssueFieldCommandRepository issueFieldCommandRepo;
    private final EnumFieldOptionCommandRepository fieldOptionCommandRepo;
    private final EnumFieldOptionQueryRepository fieldOptionQueryRepo;

    private final IssueTypeValidator issueTypeValidator;
    private final FieldDefintionPolicy fieldDefintionPolicy;

    private final EntityManager entityManager;
    private final ProjectAuthorizationService projectAuthService;

    @Override
    public IssueFieldResponse create(CreateIssueFieldCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        IssueType issueType = issueTypeFinder.getBy(cmd.issueTypeId(), project);

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueType);
        issueTypeValidator.ensureUniqueFieldLabel(issueType, cmd.name());

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
    public void rename(RenameIssueFieldCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        IssueType issueType = issueTypeFinder.getBy(cmd.issueTypeId(), project);
        IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueType);

        if (labelUnchanged(issueField.getName(), cmd.name())) {
            return;
        }

        issueTypeValidator.ensureUniqueFieldLabel(issueType, cmd.name());
        issueField.rename(cmd.name());
    }

    @Override
    public void update(PatchIssueFieldCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        IssueType issueType = issueTypeFinder.getBy(cmd.issueTypeId(), project);
        IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueType);

        Patchers.apply(cmd.description(), issueField::updateDescription);
        Patchers.apply(cmd.required(), issueField::setRequired);
    }

    @Override
    public void delete(DeleteIssueFieldCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        IssueType issueType = issueTypeFinder.getBy(cmd.issueTypeId(), project);
        IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueType);
        issueTypeValidator.ensureFieldDeletable(issueField);

        issueFieldCommandRepo.delete(issueField);
    }

    @Override
    public IssueFieldResponse addOption(AddOptionCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        IssueType issueType = issueTypeFinder.getBy(cmd.issueTypeId(), project);
        IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueType);
        issueTypeValidator.ensureUniqueOptionLabel(issueField, cmd.name());

        int nextPosition = fieldOptionQueryRepo.countByIssueField(issueField);
        fieldDefintionPolicy.ensureCanAddOption(nextPosition);

        EnumFieldOption option = EnumFieldOption.create(issueField, cmd.name(), nextPosition);
        fieldOptionCommandRepo.save(option);

        return IssueFieldResponse.from(issueField, issueType);
    }

    @Override
    public void renameOption(RenameOptionCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        IssueType issueType = issueTypeFinder.getBy(cmd.issueTypeId(), project);
        IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);
        EnumFieldOption option = fieldOptionFinder.findByIdAndIssueField(cmd.optionId(), issueField);

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueType);

        if (labelUnchanged(option.getName(), cmd.name())) {
            return;
        }

        issueTypeValidator.ensureUniqueOptionLabel(issueField, cmd.name());

        option.rename(cmd.name());
    }

    @Override
    public ReorderedOptionsResponse reorderOptions(ReorderOptionsCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        IssueType issueType = issueTypeFinder.getBy(cmd.issueTypeId(), project);
        IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueType);

        EnumFieldOptions options =
                EnumFieldOptions.fromCurrentOptions(issueField, fieldOptionFinder.findActiveOptions(issueField));

        options.ensureExactActiveIds(cmd.targetOrderedIds());
        options.bumpPositions();

        entityManager.flush();

        options.reorderTo(cmd.targetOrderedIds());

        return ReorderedOptionsResponse.from(issueField.getId(), options.getSortedOptions());
    }

    @Override
    public void deleteOption(DeleteOptionCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        IssueType issueType = issueTypeFinder.getBy(cmd.issueTypeId(), project);
        IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);
        EnumFieldOption option = fieldOptionFinder.findByIdAndIssueField(cmd.optionId(), issueField);

        projectAuthService.requireIssueTypeEditPermission(actorContext, issueType);
        issueTypeValidator.ensureOptionDeletable(option);

        fieldOptionCommandRepo.delete(option);
    }

    private boolean labelUnchanged(Name currentName, Name newName) {
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
