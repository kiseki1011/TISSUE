package com.tissue.feature.issuetype.web.request;

import jakarta.validation.constraints.NotBlank;

public record AddOptionRequest(@NotBlank String optionName) {}
