package com.tissue.issuetype.adapter.in.dto.request;

import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.application.dto.request.AddOptionCommand;
import jakarta.validation.constraints.NotBlank;

public record AddOptionRequest(@NotBlank @LabelSize String optionName) {
    public AddOptionCommand toCommand(String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId) {
        return AddOptionCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueTypeId(issueTypeId)
                .issueFieldId(issueFieldId)
                .name(Name.of(optionName))
                .build();
    }
}
