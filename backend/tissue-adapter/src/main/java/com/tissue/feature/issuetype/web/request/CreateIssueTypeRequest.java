package com.tissue.feature.issuetype.web.request;

import static com.tissue.feature.issuetype.domain.policy.IssueTypeConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.issuetype.domain.policy.IssueTypeConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.issuetype.domain.policy.IssueTypeConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateIssueTypeRequest(
        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        String name,

        @Nullable @Size(max = DESCRIPTION_MAX_LENGTH) String description,
        @NotNull ColorType color,
        @NotNull IssueHierarchy issueHierarchy,
        @NotNull Long workflowId) {

    public CreateIssueTypeCommand toCommand() {
        return CreateIssueTypeCommand.builder()
                .name(Name.of(name))
                .description(description)
                .color(color)
                .issueHierarchy(issueHierarchy)
                .workflowId(workflowId)
                .build();
    }
}
