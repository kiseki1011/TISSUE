package com.tissue.feature.tag.web.request;

import jakarta.validation.constraints.NotBlank;

public record RenameTagRequest(@NotBlank String name) {}
