package com.tissue.issuetype.web.request;

import jakarta.validation.constraints.NotBlank;

public record RenameOptionRequest(@NotBlank String name) {}
