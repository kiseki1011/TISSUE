package com.tissue.feature.issuetype.web.request;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.vo.Name;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CreateIssueFieldRequest(
        @NotBlank String name,
        @Nullable String description,
        @NotNull IssueFieldType type,
        boolean required,
        @Nullable List<String> initialOptions,
        @NotNull @Min(0) Integer position) {

    public CreateIssueFieldCommand toCommand() {
        return new CreateIssueFieldCommand(
                Name.of(name),
                description,
                type,
                required,
                initialOptions != null ? initialOptions.stream().map(Name::of).toList() : List.of(),
                position);
    }
}
