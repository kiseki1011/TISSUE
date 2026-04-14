package com.tissue.feature.issue.application.dto.request;

import java.util.Set;

public record BatchDeleteCommand(Set<String> issueKeys) {}
