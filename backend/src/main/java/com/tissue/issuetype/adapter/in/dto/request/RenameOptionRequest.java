package com.tissue.issuetype.adapter.in.dto.request;

import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.application.dto.request.RenameOptionCommand;
import jakarta.validation.constraints.NotBlank;

public record RenameOptionRequest(@NotBlank @LabelSize String name) {
    public RenameOptionCommand toCommand(
            String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId, Long optionId) {
        return RenameOptionCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueTypeId(issueTypeId)
                .issueFieldId(issueFieldId)
                .optionId(optionId)
                .name(Name.of(name))
                .build();
    }
}
