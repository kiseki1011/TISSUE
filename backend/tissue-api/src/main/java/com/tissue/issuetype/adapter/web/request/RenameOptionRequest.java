package com.tissue.issuetype.adapter.web.request;

import jakarta.validation.constraints.NotBlank;

public record RenameOptionRequest(@NotBlank String name) {}
