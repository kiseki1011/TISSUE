package com.tissue.feature.project.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AddProjectMembersRequest(@NotNull @NotEmpty Set<Long> targetMemberIds) {}
