package com.tissue.feature.issue.application.dto.request;

import java.util.Set;

public record BatchChangeParentCommand(Set<String> issueKeys, String parentIssueKey) {}
