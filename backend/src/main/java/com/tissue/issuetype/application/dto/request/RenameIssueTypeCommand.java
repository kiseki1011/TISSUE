package com.tissue.issuetype.application.dto.request;

import com.tissue.common.vo.Name;
import lombok.Builder;

@Builder
public record RenameIssueTypeCommand(String workspaceKey, String projectKey, Long issueTypeId, Name name) {}
