package com.tissue.feature.project.application.dto.request;

import lombok.Builder;

@Builder
public record CreateProjectCommand(String projectKey, String title, String description) {}
