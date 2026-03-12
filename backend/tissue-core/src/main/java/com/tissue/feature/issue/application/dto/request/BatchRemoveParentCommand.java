package com.tissue.feature.issue.application.dto.request;

import java.util.Set;

public record BatchRemoveParentCommand(Set<String> issueKeys) {}
