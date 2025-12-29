package com.tissue.issuetype.application.dto.request;

import com.tissue.common.vo.Name;
import lombok.Builder;

@Builder
public record RenameOptionCommand(
        String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId, Long optionId, Name name) {}
