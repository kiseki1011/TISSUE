package com.tissue.issuetype.adapter.web.request;

import com.tissue.global.vo.Name;
import com.tissue.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record CreateIssueFieldRequest(
        @NotBlank String name,
        @Nullable @Size(max = 255) String description,
        @NotNull IssueFieldType issueFieldType,
        @NotNull Boolean required,
        @Nullable @Size(max = 100) List<@NotBlank String> initialOptions) {

    public CreateIssueFieldCommand toCommand() {
        return CreateIssueFieldCommand.builder()
                .name(Name.of(name))
                .description(description)
                .issueFieldType(issueFieldType)
                .required(required)
                .initialOptions(toUniqueNames(initialOptions))
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
