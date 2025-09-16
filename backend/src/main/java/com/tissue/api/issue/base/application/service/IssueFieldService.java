package com.tissue.api.issue.base.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.issue.base.application.dto.AddOptionCommand;
import com.tissue.api.issue.base.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issue.base.application.dto.DeleteIssueFieldCommand;
import com.tissue.api.issue.base.application.dto.PatchIssueFieldCommand;
import com.tissue.api.issue.base.application.dto.RenameIssueFieldCommand;
import com.tissue.api.issue.base.application.dto.RenameOptionCommand;
import com.tissue.api.issue.base.application.dto.ReorderOptionsCommand;
import com.tissue.api.issue.base.application.finder.IssueFieldFinder;
import com.tissue.api.issue.base.application.finder.IssueTypeFinder;
import com.tissue.api.issue.base.application.validator.EnumFieldOptionValidator;
import com.tissue.api.issue.base.application.validator.IssueFieldValidator;
import com.tissue.api.issue.base.domain.enums.FieldType;
import com.tissue.api.issue.base.domain.model.EnumFieldOption;
import com.tissue.api.issue.base.domain.model.EnumFieldOptions;
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
			saveInitialEnumOptions(savedField, cmd.initialOptions());
		}

		return IssueFieldResponse.from(savedField);
	}

	@Transactional
	public IssueFieldResponse rename(RenameIssueFieldCommand cmd) {
		IssueType type = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeId());
		IssueField field = issueFieldFinder.findByTypeAndId(type, cmd.issueFieldId());

		if (labelUnchanged(field.getLabel(), cmd.label())) {
			return IssueFieldResponse.from(field);
		}

		issueFieldValidator.ensureUniqueLabel(type, cmd.label());
		field.rename(cmd.label());

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse patch(PatchIssueFieldCommand cmd) {
		IssueType type = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeId());
		IssueField field = issueFieldFinder.findByTypeAndId(type, cmd.issueFieldId());

		Patchers.apply(cmd.description(), field::updateDescription);
		Patchers.apply(cmd.required(), field::setRequired);

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse softDelete(DeleteIssueFieldCommand cmd) {
		IssueType type = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeId());
		IssueField field = issueFieldFinder.findByTypeAndId(type, cmd.issueFieldId());

		EnumFieldOptions options = EnumFieldOptions.fromActiveOrdered(field, findActiveOptions(field));
		options.softDeleteAll();

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

		EnumFieldOptions options = EnumFieldOptions.fromActiveOrdered(field, findActiveOptions(field));
		options.resequenceTo(cmd.orderedIds());

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

		// TODO: 해당 EnumFieldOption을 사용하는 IssueFieldValue가 있어도 soft-delete 허용할까?
		// optionValidator.ensureNotInUse(option);
		option.softDelete();

		return IssueFieldResponse.from(field);
	}

	private boolean labelUnchanged(String currentLabel, String newLabel) {
		return Objects.equals(currentLabel, newLabel);
	}

	private IssueField findIssueField(String workspaceKey, Long typeId, Long fieldId) {
		IssueType type = issueTypeFinder.findIssueType(workspaceKey, typeId);
		return issueFieldFinder.findByTypeAndId(type, fieldId);
	}

	private EnumFieldOption findOption(IssueField field, Long optionId) {
		return optionRepo.findByFieldAndId(field, optionId)
			.orElseThrow(() -> new EntityNotFoundException("Option not found."));
	}

	private List<EnumFieldOption> findActiveOptions(IssueField field) {
		return optionRepo.findByFieldOrderByPositionAsc(field);
	}

	private void saveInitialEnumOptions(IssueField field, List<String> labels) {
		int pos = 0;
		List<EnumFieldOption> options = new ArrayList<>(labels.size());
		for (String label : labels) {
			options.add(EnumFieldOption.create(field, label, pos++));
		}
		optionRepo.saveAll(options);
	}
}
