package com.tissue.issuetype.application.dto.request;

import com.tissue.common.vo.Name;
import lombok.Builder;

@Builder
public record AddOptionCommand(
        String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId, Name name) {}
