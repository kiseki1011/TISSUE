package com.tissue.feature.issue.application.dto.request;

import java.util.Set;

public record BatchSoftDeleteCommand(Set<String> issueKeys) {}
