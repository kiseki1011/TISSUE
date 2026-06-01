package com.tissue.feature.issuetype.adapter.web.request;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.vo.Name;
import io.swagger.v3.oas.annotations.media.Schema;
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

        @Schema(description = "Option names for SELECT_OPTION or CHECKLIST fields. Ignored for other types.") @Nullable
        List<String> initialOptions,

        @Schema(description = "Display order among fields in the issue type. Starts from 0.") @NotNull @Min(0)
        Integer position) {

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
