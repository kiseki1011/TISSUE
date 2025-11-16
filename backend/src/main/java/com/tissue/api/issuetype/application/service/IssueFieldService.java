package com.tissue.api.issuetype.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issuetype.application.dto.AddOptionCommand;
import com.tissue.api.issuetype.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issuetype.application.dto.PatchIssueFieldCommand;
import com.tissue.api.issuetype.application.dto.RenameIssueFieldCommand;
import com.tissue.api.issuetype.application.dto.RenameOptionCommand;
import com.tissue.api.issuetype.application.dto.ReorderOptionsCommand;
import com.tissue.api.issuetype.application.service.finder.IssueFieldFinder;
import com.tissue.api.issuetype.application.service.finder.IssueFieldOptionFinder;
import com.tissue.api.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.api.issuetype.domain.EnumFieldOption;
import com.tissue.api.issuetype.domain.EnumFieldOptions;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.domain.enums.FieldType;
import com.tissue.api.issuetype.domain.policy.FieldDefintionPolicy;
import com.tissue.api.issuetype.domain.service.validator.EnumFieldOptionValidator;
import com.tissue.api.issuetype.domain.service.validator.IssueFieldValidator;
import com.tissue.api.issuetype.presentation.dto.response.IssueFieldResponse;
import com.tissue.api.issuetype.repository.EnumFieldOptionCommandRepository;
import com.tissue.api.issuetype.repository.EnumFieldOptionQueryRepository;
import com.tissue.api.issuetype.repository.IssueFieldCommandRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueFieldService {

	private final IssueTypeFinder typeFinder;
	private final IssueFieldFinder fieldFinder;
	private final IssueFieldOptionFinder fieldOptionFinder;

	private final IssueFieldCommandRepository fieldCommandRepo;
	private final EnumFieldOptionCommandRepository fieldOptionCommandRepo;
	private final EnumFieldOptionQueryRepository fieldOptionQueryRepo;

	private final IssueFieldValidator fieldValidator;
	private final EnumFieldOptionValidator optionValidator;
	private final FieldDefintionPolicy fieldDefintionPolicy;

	private final EntityManager entityManager;

	@Transactional
	public IssueFieldResponse create(CreateIssueFieldCommand cmd) {
		IssueType issueType = typeFinder.findByIdAndWorkspaceKey(cmd.issueTypeId(), cmd.workspaceKey());

		fieldValidator.ensureUniqueLabel(issueType, cmd.label());

		IssueField issueField = IssueField.create(
			cmd.label(),
			cmd.description(),
			cmd.fieldType(),
			cmd.required(),
			issueType
		);

		IssueField savedField = fieldCommandRepo.save(issueField);

		if (savedField.getFieldType() == FieldType.ENUM) {
			fieldDefintionPolicy.ensureOptionsWithinLimit(cmd.initialOptions());
			saveInitialEnumOptions(savedField, cmd.initialOptions());
		}

		return IssueFieldResponse.from(savedField);
	}

	@Transactional
	public IssueFieldResponse rename(RenameIssueFieldCommand cmd) {
		IssueType type = typeFinder.findByIdAndWorkspaceKey(cmd.issueTypeId(), cmd.workspaceKey());
		IssueField field = fieldFinder.findByIdAndType(cmd.issueFieldId(), type);

		if (labelUnchanged(field.getLabel(), cmd.label())) {
			return IssueFieldResponse.from(field);
		}

		fieldValidator.ensureUniqueLabel(type, cmd.label());
		field.rename(cmd.label());

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse patch(PatchIssueFieldCommand cmd) {
		IssueType type = typeFinder.findByIdAndWorkspaceKey(cmd.issueTypeId(), cmd.workspaceKey());
		IssueField field = fieldFinder.findByIdAndType(cmd.issueFieldId(), type);

		Patchers.apply(cmd.description(), field::updateDescription);
		Patchers.apply(cmd.required(), field::setRequired);

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse softDelete(String workspaceKey, Long issueTypeId, Long issueFieldId) {
		IssueType type = typeFinder.findByIdAndWorkspaceKey(issueTypeId, workspaceKey);
		IssueField field = fieldFinder.findByIdAndType(issueFieldId, type);

		fieldValidator.ensureDeletable(field);

		fieldOptionCommandRepo.softDeleteByField(field);
		field.softDelete();

		return IssueFieldResponse.from(field);
	}

	// TODO: Add retry logic for DataIntegrityViolationException
	//  - Catch the exception of the unique constraint (field_id, position)
	//  - Concurrent request might cause duplicate position
	@Transactional
	public IssueFieldResponse addOption(AddOptionCommand cmd) {
		IssueField field = findIssueField(cmd.workspaceKey(), cmd.issueTypeId(), cmd.issueFieldId());

		optionValidator.ensureLabelUnique(field, cmd.label());

		int nextPosition = fieldOptionQueryRepo.countByIssueField(field);
		fieldDefintionPolicy.ensureCanAddOption(nextPosition);

		EnumFieldOption option = EnumFieldOption.create(field, cmd.label(), nextPosition);
		fieldOptionCommandRepo.save(option);

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse renameOption(RenameOptionCommand cmd) {
		IssueField field = findIssueField(cmd.workspaceKey(), cmd.issueTypeId(), cmd.issueFieldId());
		EnumFieldOption option = fieldOptionFinder.findByIdAndIssueField(cmd.optionId(), field);

		if (labelUnchanged(option.getLabel(), cmd.label())) {
			return IssueFieldResponse.from(field);
		}

		optionValidator.ensureLabelUnique(field, cmd.label());
		option.rename(cmd.label());

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse reorderOptions(ReorderOptionsCommand cmd) {
		IssueField field = findIssueField(cmd.workspaceKey(), cmd.issueTypeId(), cmd.issueFieldId());
		EnumFieldOptions options = EnumFieldOptions.fromCurrentOptions(field,
			fieldOptionFinder.findActiveOptions(field));

		options.ensureExactActiveIds(cmd.targetOrderedIds());
		options.bumpPositions();

		entityManager.flush();

		options.reorderTo(cmd.targetOrderedIds());

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse softDeleteOption(
		String workspaceKey,
		Long issueTypeId,
		Long issueFieldId,
		Long optionId
	) {
		IssueField field = findIssueField(workspaceKey, issueTypeId, issueFieldId);
		EnumFieldOption option = fieldOptionFinder.findByIdAndIssueField(optionId, field);

		// TODO: 해당 EnumFieldOption을 사용하는 IssueFieldValue가 있어도 soft-delete 허용할까?
		// optionValidator.ensureNotInUse(option);
		option.softDelete();

		return IssueFieldResponse.from(field);
	}

	private boolean labelUnchanged(Label currentLabel, Label newLabel) {
		return Objects.equals(currentLabel, newLabel);
	}

	private IssueField findIssueField(String workspaceKey, Long issueTypeId, Long issueFieldId) {
		IssueType issueType = typeFinder.findByIdAndWorkspaceKey(issueTypeId, workspaceKey);
		return fieldFinder.findByIdAndType(issueFieldId, issueType);
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
