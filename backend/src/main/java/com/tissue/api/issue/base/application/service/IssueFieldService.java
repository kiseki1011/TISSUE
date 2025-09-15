package com.tissue.api.issue.base.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.base.application.dto.AddOptionCommand;
import com.tissue.api.issue.base.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issue.base.application.dto.DeleteIssueFieldCommand;
import com.tissue.api.issue.base.application.dto.RenameOptionCommand;
import com.tissue.api.issue.base.application.dto.ReorderOptionsCommand;
import com.tissue.api.issue.base.application.dto.UpdateIssueFieldCommand;
import com.tissue.api.issue.base.application.finder.IssueFieldFinder;
import com.tissue.api.issue.base.application.finder.IssueTypeFinder;
import com.tissue.api.issue.base.application.validator.EnumFieldOptionValidator;
import com.tissue.api.issue.base.application.validator.IssueFieldValidator;
import com.tissue.api.issue.base.domain.enums.FieldType;
import com.tissue.api.issue.base.domain.model.EnumFieldOption;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.domain.policy.IssueFieldPolicy;
import com.tissue.api.issue.base.infrastructure.repository.EnumFieldOptionRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;
import com.tissue.api.issue.base.presentation.dto.response.IssueFieldResponse;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueFieldService {

	private final IssueTypeFinder issueTypeFinder;
	private final IssueFieldFinder issueFieldFinder;
	private final IssueFieldRepository issueFieldRepo;
	private final EnumFieldOptionRepository optionRepo;
	private final IssueFieldValidator issueFieldValidator;
	private final EnumFieldOptionValidator optionValidator;
	private final IssueFieldPolicy issueFieldPolicy;

	@Transactional
	public IssueFieldResponse create(CreateIssueFieldCommand cmd) {
		IssueType issueType = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeId());

		issueFieldValidator.ensureUniqueLabel(issueType, cmd.label());

		IssueField issueField = IssueField.create(
			cmd.label(),
			cmd.description(),
			cmd.fieldType(),
			cmd.required(),
			issueType
		);

		IssueField savedField = issueFieldRepo.save(issueField);

		if (savedField.getFieldType() == FieldType.ENUM) {
			issueFieldPolicy.ensureOptionsWithinLimit(cmd.initialOptions());
			persistInitialEnumOptions(savedField, cmd.initialOptions());
		}

		return IssueFieldResponse.from(savedField);
	}

	@Transactional
	public IssueFieldResponse updateMetaData(UpdateIssueFieldCommand cmd) {
		IssueType type = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeId());
		IssueField field = issueFieldFinder.findIssueField(type, cmd.issueFieldId());

		field.updateMetaData(cmd.description(), cmd.required());

		boolean labelChanged = !Objects.equals(field.getLabel(), cmd.label());
		if (labelChanged) {
			issueFieldValidator.ensureUniqueLabel(type, cmd.label());
			field.rename(cmd.label());
		}

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse softDelete(DeleteIssueFieldCommand cmd) {
		IssueType type = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeId());
		IssueField field = issueFieldFinder.findIssueField(type, cmd.issueFieldId());

		issueFieldValidator.ensureDeletable(field);
		field.softDelete();

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse addOption(AddOptionCommand cmd) {
		IssueField field = findIssueField(cmd.workspaceKey(), cmd.issueTypeId(), cmd.issueFieldId());

		optionValidator.ensureLabelUnique(field, cmd.label());

		int nextPosition = optionRepo.countByField(field);
		issueFieldPolicy.ensureCanAddOption(nextPosition);

		EnumFieldOption option = EnumFieldOption.create(field, cmd.label(), nextPosition);
		optionRepo.save(option);

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse renameOption(RenameOptionCommand cmd) {
		IssueField field = findIssueField(cmd.workspaceKey(), cmd.issueTypeId(), cmd.issueFieldId());
		EnumFieldOption option = findOption(field, cmd.optionId());

		boolean labelChanged = !Objects.equals(option.getLabel(), cmd.newLabel());
		if (labelChanged) {
			optionValidator.ensureLabelUnique(field, cmd.newLabel());
			option.rename(cmd.newLabel());
		}

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse reorderOptions(ReorderOptionsCommand cmd) {
		IssueField field = findIssueField(cmd.workspaceKey(), cmd.issueTypeId(), cmd.issueFieldId());

		// TODO: EnumFieldOptions라는 일급객체를 사용하도록 리팩토링 고려하자
		List<EnumFieldOption> active = findActiveOptions(field);
		optionValidator.ensureValidReorder(active, cmd.orderedIds());

		applyNewOrder(active, cmd.orderedIds());

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
		EnumFieldOption option = findOption(field, optionId);

		optionValidator.ensureNotInUse(option);
		option.softDelete();

		return IssueFieldResponse.from(field);
	}

	private IssueField findIssueField(String workspaceKey, Long typeId, Long fieldId) {
		IssueType type = issueTypeFinder.findIssueType(workspaceKey, typeId);
		return issueFieldFinder.findIssueField(type, fieldId);
	}

	private EnumFieldOption findOption(IssueField field, Long optionId) {
		return optionRepo.findByFieldAndId(field, optionId)
			.orElseThrow(() -> new EntityNotFoundException("Option not found."));
	}

	private List<EnumFieldOption> findActiveOptions(IssueField field) {
		return optionRepo.findByFieldOrderByPositionAsc(field);
	}

	private void persistInitialEnumOptions(IssueField field, List<String> labels) {
		int pos = 0;
		List<EnumFieldOption> options = new ArrayList<>(labels.size());
		for (String label : labels) {
			options.add(EnumFieldOption.create(field, label, pos++));
		}
		optionRepo.saveAll(options);
	}

	private void applyNewOrder(List<EnumFieldOption> activeOptions, List<Long> orderedIds) {
		Map<Long, EnumFieldOption> optionsById = activeOptions.stream()
			.collect(Collectors.toMap(EnumFieldOption::getId, o -> o));

		int newPosition = 0;
		for (Long optionId : orderedIds) {
			EnumFieldOption option = optionsById.get(optionId);
			if (option.getPosition() != newPosition) {
				option.movePositionTo(newPosition);
			}
			newPosition++;
		}
	}
}
