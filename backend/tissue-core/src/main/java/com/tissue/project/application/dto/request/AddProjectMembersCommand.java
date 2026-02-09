package com.tissue.project.application.dto.request;

import java.util.Set;

public record AddProjectMembersCommand(Set<Long> targetMemberIds) {}
