package com.tissue.issuetype.adapter.in.dto.request;

import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record CreateIssueFieldRequest(
        @NotBlank @LabelSize String name,
        @Nullable @Size(max = 255) String description,
        @NotNull IssueFieldType issueFieldType,
        @NotNull Boolean required,
        @Nullable @Size(max = 100) List<@NotBlank @LabelSize String> initialOptions) {

    public CreateIssueFieldCommand toCommand(Long issueTypeId, ProjectMemberContext actorContext) {
        return CreateIssueFieldCommand.builder()
                .issueTypeId(issueTypeId)
                .name(Name.of(name))
                .description(description)
                .issueFieldType(issueFieldType)
                .required(required)
                .initialOptions(toUniqueNames(initialOptions))
                .actorContext(actorContext)
                .build();
    }

    private List<Name> toUniqueNames(@Nullable List<String> raw) {
        return Optional.ofNullable(raw).orElseGet(List::of).stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .map(Name::of)
                .distinct()
                .toList();
    }
}
