package com.tissue.issuetype.adapter.web.request;

import jakarta.validation.constraints.NotBlank;

public record RenameIssueFieldRequest(@NotBlank String name) {}
