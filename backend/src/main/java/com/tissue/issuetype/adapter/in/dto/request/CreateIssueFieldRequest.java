package com.tissue.issuetype.adapter.in.dto.request;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.lang.Nullable;

import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Label;
import com.tissue.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.issuetype.domain.enums.IssueFieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueFieldRequest(
	@NotBlank @LabelSize String label,
	@Nullable @Size(max = 255) String description,
	@NotNull IssueFieldType issueFieldType,
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
			.issueFieldType(issueFieldType)
			.required(required)
			.initialOptions(toUniqueLabels(initialOptions))
			.build();
	}

	// TODO: should i consider separating this into a separate util class?
	//  currently im only using this here
	//  is it ok to make and use a util method inside a dto?
	private List<Label> toUniqueLabels(@Nullable List<String> raw) {
		return Optional.ofNullable(raw).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(s -> !s.isBlank())
			.map(Label::of)
			.distinct()
			.toList();
	}
}
