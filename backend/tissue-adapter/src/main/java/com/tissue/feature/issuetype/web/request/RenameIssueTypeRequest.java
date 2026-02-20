package com.tissue.feature.issuetype.web.request;

import jakarta.validation.constraints.NotBlank;

public record RenameIssueTypeRequest(@NotBlank String name) {}
