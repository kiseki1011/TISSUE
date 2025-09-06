package com.tissue.api.issue.base.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.CollectionNormalizer;
import com.tissue.api.issue.base.application.dto.AddOptionCommand;
import com.tissue.api.issue.base.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issue.base.application.dto.DeleteIssueFieldCommand;
import com.tissue.api.issue.base.application.dto.DeleteOptionCommand;
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
		IssueType issueType = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeKey());

		// TODO: IssueFieldFactory를 만들어서 생성 책임 분리 고려 (검증을 포함한 과정을 해당 생성 메서드에 캡슐화)
		IssueField issueField = issueFieldRepo.save(IssueField.builder()
			.label(cmd.label())
			.description(cmd.description())
			.fieldType(cmd.fieldType())
			.required(Boolean.TRUE.equals(cmd.required()))
			.issueType(issueType)
			.build());

		issueFieldValidator.ensureUniqueLabel(issueType, issueField.getLabel());

		if (issueField.getFieldType() == FieldType.ENUM) {
			List<String> normalized = normalizeInitialOptions(cmd.initialOptions());
			persistInitialEnumOptions(issueField, normalized);
		}

		return IssueFieldResponse.from(issueField);
	}

	@Transactional
	public IssueFieldResponse updateMetaData(UpdateIssueFieldCommand cmd) {
		IssueType type = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeKey());
		IssueField field = issueFieldFinder.findIssueField(type, cmd.issueFieldKey());

		field.updateMetaData(cmd.label(), cmd.description(), cmd.required());

		issueFieldValidator.ensureUniqueLabel(type, field.getLabel(), field.getId());

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse delete(DeleteIssueFieldCommand cmd) {
		IssueType type = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeKey());
		IssueField field = issueFieldFinder.findIssueField(type, cmd.issueFieldKey());

		issueFieldValidator.ensureDeletable(field);
		field.delete();

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse addOption(AddOptionCommand cmd) {
		IssueField field = findIssueField(cmd.workspaceKey(), cmd.issueTypeKey(), cmd.issueFieldKey());

		int nextPosition = optionRepo.countByField(field);
		issueFieldPolicy.ensureCanAddOption(nextPosition);

		EnumFieldOption option = EnumFieldOption.builder()
			.field(field)
			.label(cmd.label())
			.position(nextPosition)
			.build();

		optionValidator.ensureLabelUnique(field, option.getLabel());

		optionRepo.save(option);

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse renameOption(RenameOptionCommand cmd) {
		IssueField field = findIssueField(cmd.workspaceKey(), cmd.issueTypeKey(), cmd.issueFieldKey());
		EnumFieldOption option = findOption(field, cmd.optionKey());

		boolean labelHasChanged = !Objects.equals(option.getLabel(), cmd.newLabel());
		if (labelHasChanged) {
			optionValidator.ensureLabelUnique(field, cmd.newLabel());
			option.rename(cmd.newLabel());
		}

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse deleteOption(DeleteOptionCommand cmd) {
		IssueField field = findIssueField(cmd.workspaceKey(), cmd.issueTypeKey(), cmd.issueFieldKey());
		EnumFieldOption option = findOption(field, cmd.optionKey());

		optionValidator.ensureNotInUse(option);
		option.delete();

		return IssueFieldResponse.from(field);
	}

	@Transactional
	public IssueFieldResponse reorderOptions(ReorderOptionsCommand cmd) {
		IssueField field = findIssueField(cmd.workspaceKey(), cmd.issueTypeKey(), cmd.issueFieldKey());

		// TODO: EnumFieldOptions라는 일급객체를 사용하도록 리팩토링 고려
		List<EnumFieldOption> active = findActiveOptions(field);
		optionValidator.ensureValidReorder(active, cmd.orderKeys());

		applyNewOrder(active, cmd.orderKeys());

		return IssueFieldResponse.from(field);
	}

	private IssueField findIssueField(String workspaceKey, String typeKey, String fieldKey) {
		IssueType type = issueTypeFinder.findIssueType(workspaceKey, typeKey);
		return issueFieldFinder.findIssueField(type, fieldKey);
	}

	private EnumFieldOption findOption(IssueField field, String optionKey) {
		return optionRepo.findByFieldAndKey(field, optionKey)
			.orElseThrow(() -> new EntityNotFoundException("Option not found."));
	}

	private List<EnumFieldOption> findActiveOptions(IssueField field) {
		return optionRepo.findByFieldOrderByPositionAsc(field);
	}

	private List<String> normalizeInitialOptions(List<String> rawOptions) {
		List<String> normalized = CollectionNormalizer.normalizeOptions(rawOptions);
		issueFieldPolicy.ensureOptionsWithinLimit(normalized.size());
		return normalized;
	}

	private void persistInitialEnumOptions(IssueField field, List<String> labels) {
		int pos = 0;
		List<EnumFieldOption> options = new ArrayList<>(labels.size());
		for (String label : labels) {
			// optionValidator.ensureLabelUnique(field, label);
			options.add(new EnumFieldOption(field, label, pos++));
		}
		optionRepo.saveAll(options);
	}

	private void applyNewOrder(List<EnumFieldOption> activeOptions, List<String> orderedKeys) {
		Map<String, EnumFieldOption> optionsByKey = activeOptions.stream()
			.collect(Collectors.toMap(EnumFieldOption::getKey, o -> o));

		int newPosition = 0;
		for (String optionKey : orderedKeys) {
			EnumFieldOption option = optionsByKey.get(optionKey);
			if (option.getPosition() != newPosition) {
				option.movePositionTo(newPosition);
			}
			newPosition++;
		}
	}

}
