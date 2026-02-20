package com.tissue.feature.issuetype.web.request;

import static com.tissue.feature.issuetype.domain.policy.IssueTypeConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.issuetype.domain.policy.IssueTypeConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.issuetype.domain.policy.IssueTypeConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.vo.Name;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record CreateIssueFieldRequest(
        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        String name,

        @Nullable @Size(max = DESCRIPTION_MAX_LENGTH) String description,
        @NotNull IssueFieldType issueFieldType,
        @NotNull Boolean required,
        @Nullable @Size(max = 100) List<@NotBlank @Size(max = 50) String> initialOptions) {

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
