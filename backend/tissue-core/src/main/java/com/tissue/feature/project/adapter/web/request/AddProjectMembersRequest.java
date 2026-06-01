package com.tissue.feature.project.adapter.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AddProjectMembersRequest(
        @NotNull @NotEmpty @Size(max = 100) Set<Long> targetMemberIds) {}
