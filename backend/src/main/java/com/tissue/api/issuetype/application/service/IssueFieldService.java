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
import com.tissue.api.issuetype.application.dto.DeleteIssueFieldCommand;
import com.tissue.api.issuetype.application.dto.DeleteOptionCommand;
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
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.domain.Project;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueFieldService {

	private final ProjectFinder projectFinder;
	private final IssueTypeFinder issueTypeFinder;
	private final IssueFieldFinder issueFieldFinder;
	private final IssueFieldOptionFinder fieldOptionFinder;

	private final IssueFieldCommandRepository issueFieldCommandRepo;
	private final EnumFieldOptionCommandRepository fieldOptionCommandRepo;
	private final EnumFieldOptionQueryRepository fieldOptionQueryRepo;

	private final IssueFieldValidator issueFieldValidator;
	private final EnumFieldOptionValidator fieldOptionValidator;
	private final FieldDefintionPolicy fieldDefintionPolicy;

	private final EntityManager entityManager;

	@Transactional
	public IssueFieldResponse create(CreateIssueFieldCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());

		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);

		issueFieldValidator.ensureUniqueLabel(issueType, cmd.label());

		IssueField issueField = IssueField.create(
			cmd.label(),
			cmd.description(),
			cmd.fieldType(),
			cmd.required(),
			issueType
		);

		IssueField savedField = issueFieldCommandRepo.save(issueField);

		if (savedField.getFieldType() == FieldType.ENUM) {
			fieldDefintionPolicy.ensureOptionsWithinLimit(cmd.initialOptions());
			saveInitialEnumOptions(savedField, cmd.initialOptions());
		}

		return IssueFieldResponse.from(savedField);
	}

	@Transactional
	public IssueFieldResponse rename(RenameIssueFieldCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

		if (labelUnchanged(issueField.getLabel(), cmd.label())) {
			return IssueFieldResponse.from(issueField);
		}

		issueFieldValidator.ensureUniqueLabel(issueType, cmd.label());
		issueField.rename(cmd.label());

		return IssueFieldResponse.from(issueField);
	}

	@Transactional
	public IssueFieldResponse update(PatchIssueFieldCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

		Patchers.apply(cmd.description(), issueField::updateDescription);
		Patchers.apply(cmd.required(), issueField::setRequired);

		return IssueFieldResponse.from(issueField);
	}

	// TODO: hard-delete 정책 사용
	@Transactional
	public IssueFieldResponse delete(DeleteIssueFieldCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

		// TODO: 해당 IssueField를 사용해서 값을 설정한 이슈가 단 하나라도 있으면 삭제 불가
		issueFieldValidator.ensureDeletable(issueField);

		fieldOptionCommandRepo.softDeleteByField(issueField);
		issueField.softDelete();

		return IssueFieldResponse.from(issueField);
	}

	@Transactional
	public IssueFieldResponse addOption(AddOptionCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);

		fieldOptionValidator.ensureLabelUnique(issueField, cmd.label());

		int nextPosition = fieldOptionQueryRepo.countByIssueField(issueField);
		fieldDefintionPolicy.ensureCanAddOption(nextPosition);

		EnumFieldOption option = EnumFieldOption.create(issueField, cmd.label(), nextPosition);
		fieldOptionCommandRepo.save(option);

		return IssueFieldResponse.from(issueField);
	}

	@Transactional
	public IssueFieldResponse renameOption(RenameOptionCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);
		EnumFieldOption option = fieldOptionFinder.findByIdAndIssueField(cmd.optionId(), issueField);

		if (labelUnchanged(option.getLabel(), cmd.label())) {
			return IssueFieldResponse.from(issueField);
		}

		fieldOptionValidator.ensureLabelUnique(issueField, cmd.label());
		option.rename(cmd.label());

		return IssueFieldResponse.from(issueField);
	}

	@Transactional
	public IssueFieldResponse reorderOptions(ReorderOptionsCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
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

		return IssueFieldResponse.from(issueField);
	}

	// TODO: hard-delete 정책으로 변경
	@Transactional
	public IssueFieldResponse deleteOption(DeleteOptionCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		IssueField issueField = issueFieldFinder.findBy(cmd.issueFieldId(), issueType);
		EnumFieldOption option = fieldOptionFinder.findByIdAndIssueField(cmd.optionId(), issueField);

		// TODO: 해당 option을 값으로 사용하는 이슈가 단 하나라도 있으면 삭제 불가
		// optionValidator.ensureNotInUse(option);
		option.softDelete();

		return IssueFieldResponse.from(issueField);
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
