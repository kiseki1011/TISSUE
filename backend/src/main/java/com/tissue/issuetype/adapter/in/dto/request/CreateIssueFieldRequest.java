package com.tissue.issuetype.adapter.in.dto.request;

import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.common.util.CollectionNormalizer;
import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Label;
import com.tissue.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.issuetype.domain.enums.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueFieldRequest(
	@NotBlank @LabelSize String label,
	@Nullable @Size(max = 255) String description,
	@NotNull FieldType fieldType,
	@NotNull Boolean required,
	@Nullable @Size(max = 100) List<@NotBlank @LabelSize String> initialOptions
) {
	public CreateIssueFieldCommand toCommand(
		String workspaceKey,
		String projectKey,
		Long issueTypeId
	) {
		return CreateIssueFieldCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.issueTypeId(issueTypeId)
			.label(Label.of(label))
			.description(description)
			.fieldType(fieldType)
			.required(required)
			.initialOptions(CollectionNormalizer.toUniqueLabels(initialOptions))
			.build();
	}
}
