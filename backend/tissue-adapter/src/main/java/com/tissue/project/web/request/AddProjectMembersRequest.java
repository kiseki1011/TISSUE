package com.tissue.project.web.request;

import java.util.Set;

public record AddProjectMembersRequest(Set<Long> targetMemberIds) {}
