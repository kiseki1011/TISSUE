package com.tissue.issuetype.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.common.util.Patchers;
import com.tissue.common.vo.Label;
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
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

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

	@Override
	public IssueFieldResponse create(CreateIssueFieldCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);

		issueTypeValidator.ensureUniqueFieldLabel(issueType, cmd.label());

		IssueField issueField = IssueField.create(
			cmd.label(),
			cmd.description(),
			cmd.issueFieldType(),
			cmd.required(),
			issueType
		);

		IssueField savedField = issueFieldCommandRepo.save(issueField);

		if (savedField.getIssueFieldType() == IssueFieldType.ENUM) {
			fieldDefintionPolicy.ensureOptionsWithinLimit(cmd.initialOptions());
			saveInitialEnumOptions(savedField, cmd.initialOptions());
		}

		return IssueFieldResponse.from(savedField, issueType);
	}

	@Override
	public void rename(RenameIssueFieldCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

		if (labelUnchanged(issueField.getLabel(), cmd.label())) {
			return;
		}

		issueTypeValidator.ensureUniqueFieldLabel(issueType, cmd.label());
		issueField.rename(cmd.label());
	}

	@Override
	public void update(PatchIssueFieldCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

		Patchers.apply(cmd.description(), issueField::updateDescription);
		Patchers.apply(cmd.required(), issueField::setRequired);
	}

	@Override
	public void delete(DeleteIssueFieldCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

		issueTypeValidator.ensureFieldDeletable(issueField);

		issueFieldCommandRepo.delete(issueField);
	}

	@Override
	public IssueFieldResponse addOption(AddOptionCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

		issueTypeValidator.ensureUniqueOptionLabel(issueField, cmd.label());

		int nextPosition = fieldOptionQueryRepo.countByIssueField(issueField);
		fieldDefintionPolicy.ensureCanAddOption(nextPosition);

		EnumFieldOption option = EnumFieldOption.create(issueField, cmd.label(), nextPosition);
		fieldOptionCommandRepo.save(option);

		return IssueFieldResponse.from(issueField, issueType);
	}

	@Override
	public void renameOption(RenameOptionCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);
		EnumFieldOption option = fieldOptionFinder.findByIdAndIssueField(cmd.optionId(), issueField);

		if (labelUnchanged(option.getLabel(), cmd.label())) {
			return;
		}

		issueTypeValidator.ensureUniqueOptionLabel(issueField, cmd.label());
		option.rename(cmd.label());
	}

	@Override
	public ReorderedOptionsResponse reorderOptions(ReorderOptionsCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

		EnumFieldOptions options = EnumFieldOptions.fromCurrentOptions(
			issueField,
			fieldOptionFinder.findActiveOptions(issueField)
		);

		options.ensureExactActiveIds(cmd.targetOrderedIds());
		options.bumpPositions();

		entityManager.flush();

		options.reorderTo(cmd.targetOrderedIds());

		return ReorderedOptionsResponse.from(issueField.getId(), options.getSortedOptions());
	}

	@Override
	public void deleteOption(DeleteOptionCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);
		EnumFieldOption option = fieldOptionFinder.findByIdAndIssueField(cmd.optionId(), issueField);

		issueTypeValidator.ensureOptionDeletable(option);

		fieldOptionCommandRepo.delete(option);
	}

	private boolean labelUnchanged(Label currentLabel, Label newLabel) {
		return Objects.equals(currentLabel, newLabel);
	}

	private void saveInitialEnumOptions(IssueField field, List<Label> labels) {
		int pos = 0;
		List<EnumFieldOption> options = new ArrayList<>(labels.size());
		for (Label label : labels) {
			options.add(EnumFieldOption.create(field, label, pos++));
		}
		fieldOptionCommandRepo.saveAll(options);
	}
}
